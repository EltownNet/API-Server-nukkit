package net.eltown.apiserver.components.handler.player;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.economy.EconomyCalls;
import net.eltown.apiserver.components.handler.economy.EconomyProvider;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PlayerHandler {

    private final Channel callbackChannel, receivingChannel;
    private final Server server;
    private final PlayerProvider provider;

    @SneakyThrows
    public PlayerHandler(final Server server, final Connection connection) {
        this.server = server;
        this.provider = new PlayerProvider(server);
        this.callbackChannel = connection.createChannel();
        this.callbackChannel.queueDeclare("playersync", false, false, false, null);
        this.callbackChannel.queuePurge("playersync");
        this.callbackChannel.basicQos(1);

        this.receivingChannel = connection.createChannel();
        this.receivingChannel.queueDeclare("playersyncReceive", false, false, false, null);

        this.startCallbacking();
        this.startReceiving();
    }

    public void startReceiving() {
        this.server.getExecutor().execute(() -> {
            try {
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    final String[] request = message.split("//");

                    this.server.log("[" + Thread.currentThread().getName() + "] " + "[«] " + request[0].toUpperCase());

                    switch (PlayerCalls.valueOf(request[0].toUpperCase())) {
                        case REQUEST_SETSYNC:
                            SyncPlayer set = new SyncPlayer(
                                    request[2],
                                    request[3],
                                    request[4],
                                    request[5],
                                    request[6],
                                    request[7],
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
                };

                this.receivingChannel.basicConsume("playersyncReceive", true, deliverCallback, consumerTag -> {
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            try {
                final DeliverCallback callback = (tag, delivery) -> {
                    final AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(delivery.getProperties().getCorrelationId())
                            .build();

                    final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    final String[] request = message.split("//");

                    this.server.log("[" + Thread.currentThread().getName() + "] " + "[«] " + request[0].toUpperCase());

                    switch (PlayerCalls.valueOf(request[0].toUpperCase())) {
                        case REQUEST_SYNC:
                            final SyncPlayer syncPlayer = this.provider.get(request[1]);
                            if (syncPlayer.isCanSync()) {
                                this.publish(delivery, replyProps, PlayerCalls.GOT_SYNC.name() + "//" + syncPlayer.getInvString() + "//" + syncPlayer.getEcString() + "//" + syncPlayer.getHealth() + "//" + syncPlayer.getFood() + "//" + syncPlayer.getExp() + "//" + syncPlayer.getLevel());
                            } else {
                                this.publish(delivery, replyProps, PlayerCalls.GOT_NOSYNC.name() + "//" + request[1]);
                            }
                            break;
                    }
                };

                this.callbackChannel.basicConsume("playersync", false, callback, (consumerTag -> {
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @SneakyThrows
    private void publish(final Delivery delivery, final AMQP.BasicProperties props, String message) {
        this.callbackChannel.basicPublish("", delivery.getProperties().getReplyTo(), props, message.getBytes(StandardCharsets.UTF_8));
        this.callbackChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        this.server.log("[" + Thread.currentThread().getName() + "] " + "[»] " + message.split("//")[0]);
    }

}
