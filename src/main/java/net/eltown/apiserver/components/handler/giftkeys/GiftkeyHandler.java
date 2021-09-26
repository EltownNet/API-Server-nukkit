package net.eltown.apiserver.components.handler.giftkeys;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.giftkeys.data.Giftkey;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.Arrays;
import java.util.List;

public class GiftkeyHandler {

    private final Server server;
    private final GiftkeyProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public GiftkeyHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new GiftkeyProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_DELETE_KEY:
                        this.provider.deleteKey(d[1]);
                        break;
                }
            }, "API/Giftkeys[Receive]", "api.giftkeys.receive");
        });

        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                final String[] d = request.getData();
                switch (GiftkeyCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CREATE_KEY:
                        final int maxUses = Integer.parseInt(d[1]);
                        final List<String> rewards = Arrays.asList(d[2].split(">:<"));
                        this.provider.createKey(maxUses, rewards, key -> {
                            request.answer(GiftkeyCalls.CALLBACK_NULL.name(), key);
                        });
                        break;
                    case REQUEST_GET_KEY:
                        final String key = d[1];
                        if (!this.provider.keyExists(key)) {
                            request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                            return;
                        }
                        final Giftkey giftkey = this.provider.getGiftKey(key);
                        final StringBuilder builder = new StringBuilder(key).append(">>").append(giftkey.getMaxUses()).append(">>");

                        String a = "null";
                        if (giftkey.getUses().size() != 0) {
                            final StringBuilder usesBuilder = new StringBuilder();
                            giftkey.getUses().forEach(e -> {
                                usesBuilder.append(e).append(">:<");
                            });
                            a = usesBuilder.substring(0, usesBuilder.length() - 3);
                        }
                        builder.append(a).append(">>");

                        final StringBuilder rewardsBuilder = new StringBuilder();
                        giftkey.getRewards().forEach(e -> {
                            rewardsBuilder.append(e).append(">:<");
                        });
                        builder.append(rewardsBuilder.substring(0, rewardsBuilder.length() - 3));

                        request.answer(GiftkeyCalls.CALLBACK_KEY.name(), builder.toString());
                        break;
                    case REQUEST_REDEEM_KEY:
                        final String redeemKey = d[1];
                        final String player = d[2];
                        if (this.provider.keyExists(redeemKey)) {
                            if (!this.provider.alreadyRedeemed(redeemKey, player)) {
                                this.provider.redeemKey(redeemKey, player);

                                final Giftkey giftkey1 = this.provider.getGiftKey(redeemKey);
                                final StringBuilder redeemBuilder = new StringBuilder();
                                giftkey1.getRewards().forEach(e -> {
                                    redeemBuilder.append(e).append(">:<");
                                });
                                final String redeemRewards = redeemBuilder.substring(0, redeemBuilder.length() - 3);
                                request.answer(GiftkeyCalls.CALLBACK_REDEEMED.name(), redeemRewards);
                            } else request.answer(GiftkeyCalls.CALLBACK_ALREADY_REDEEMED.name(), "null");
                        } else request.answer(GiftkeyCalls.CALLBACK_NULL.name(), "null");
                        break;
                }
            }), "API/Giftkeys[Callback]", "api.giftkeys.callback");
        });
    }

}
