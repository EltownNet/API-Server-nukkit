package net.eltown.apiserver.components.handler.friends;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.friends.data.FriendData;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class FriendHandler {

    private final Server server;
    private final FriendProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public FriendHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new FriendProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (FriendCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_FRIEND_DATA:
                        if (!this.provider.friendDataExists(d[1])) this.provider.createFriendData(d[1]);
                        break;
                    case REQUEST_CREATE_FRIEND_REQUEST:
                        this.provider.createFriendRequest(d[1], d[2]);
                        break;
                    case REQUEST_REMOVE_FRIEND_REQUEST:
                        this.provider.removeFriendRequest(d[1], d[2]);
                        break;
                    case REQUEST_CREATE_FRIENDSHIP:
                        this.provider.createFriendship(d[1], d[2]);
                        break;
                    case REQUEST_REMOVE_FRIENDSHIP:
                        this.provider.removeFriendship(d[1], d[2]);
                        break;
                }
            }, "API/Friends[Receive]", "api.friends.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                final String[] d = request.getData();
                switch (FriendCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CHECK_ARE_FRIENDS:
                        request.answer(FriendCalls.CALLBACK_ARE_FRIENDS.name(), String.valueOf(this.provider.areFriends(d[1], d[2])));
                        break;
                    case REQUEST_CHECK_REQUEST_EXISTS:
                        request.answer(FriendCalls.CALLBACK_REQUEST_EXISTS.name(), String.valueOf(this.provider.requestExists(d[1], d[2])));
                        break;
                    case REQUEST_FRIEND_DATA:
                        if (!this.provider.friendDataExists(d[1])) {
                            request.answer(FriendCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }

                        final FriendData data = this.provider.cachedFriendData.get(d[1]);

                        final StringBuilder friendBuilder = new StringBuilder();
                        data.getFriends().forEach(e -> {
                            friendBuilder.append(e).append(":");
                        });
                        if (friendBuilder.toString().isEmpty()) friendBuilder.append("null:");
                        final String friends = friendBuilder.substring(0, friendBuilder.length() - 1);

                        final StringBuilder requestBuilder = new StringBuilder();
                        data.getRequests().forEach(e -> {
                            requestBuilder.append(e).append(":");
                        });
                        if (requestBuilder.toString().isEmpty()) requestBuilder.append("null:");
                        final String requests = requestBuilder.substring(0, requestBuilder.length() - 1);

                        request.answer(FriendCalls.CALLBACK_FRIEND_DATA.name(), friends, requests);
                        break;
                }
            }, "API/Friends[Callback]", "api.friends.callback");
        });
    }
}
