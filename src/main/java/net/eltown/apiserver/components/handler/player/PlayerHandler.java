package net.eltown.apiserver.components.handler.player;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class PlayerHandler {

    private final TinyRabbitListener listener;
    private final Server server;
    private final PlayerProvider provider;

    @SneakyThrows
    public PlayerHandler(final Server server, final Connection connection) {
        this.server = server;
        this.provider = new PlayerProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.listener.throwExceptions(true);

        this.startCallbacking();
        this.startReceiving();
    }

    public void startReceiving() {
        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery -> {
                final String[] request = delivery.getData();

                switch (PlayerCalls.valueOf(delivery.getKey())) {
                    case REQUEST_SETSYNC:
                        SyncPlayer set = new SyncPlayer(
                                request[2],
                                request[3],
                                request[4],
                                request[5],
                                request[6],
                                request[7],
                                request[8],
                                request[9],
                                true
                        );
                        this.provider.set(request[1], set);
                        break;
                    case REQUEST_SETNOSYNC:
                        SyncPlayer player = this.provider.get(request[1]);
                        player.setCanSync(false);
                        this.provider.set(request[1], player);
                        break;
                }

            }), "API Server", "playersyncReceive");
        });
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.listener.callback((request -> {
                switch (PlayerCalls.valueOf(request.getKey())) {
                    case REQUEST_SYNC:
                        final SyncPlayer syncPlayer = this.provider.get(request.getData()[1]);
                        if (syncPlayer.isCanSync()) {
                            request.answer(PlayerCalls.GOT_SYNC.name(), syncPlayer.getInvString() + "//" + syncPlayer.getEcString() + "//" + syncPlayer.getHealth() + "//" + syncPlayer.getFood() + "//" + syncPlayer.getExp() + "//" + syncPlayer.getLevel() + "//" + syncPlayer.getEffects() + "//" + syncPlayer.getGamemode());
                        } else {
                            request.answer(PlayerCalls.GOT_NOSYNC.name(), request.getData()[1]);
                        }
                        break;
                }
            }), "API Server", "playersync");
        });
    }

}
