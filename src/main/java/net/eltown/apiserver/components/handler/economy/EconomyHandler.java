package net.eltown.apiserver.components.handler.economy;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;

import java.nio.charset.StandardCharsets;

public class EconomyHandler {

    private final Channel channel;
    private final Server server;
    private final EconomyProvider provider;

    @SneakyThrows
    public EconomyHandler(final Server server, final Connection connection) {
        this.server = server;
        this.provider = new EconomyProvider(server);
        this.channel = connection.createChannel();
        this.channel.queueDeclare("economy", false, false, false, null);
        this.channel.queuePurge("economy");
        this.channel.basicQos(1);

        this.startCallbacking();
    }

    public void startCallbacking() {
        server.getExecutor().execute(() -> {
            try {
                final DeliverCallback callback = (tag, delivery) -> {
                    final AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(delivery.getProperties().getCorrelationId())
                            .build();

                    final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    final String[] request = message.split("//");

                    this.server.log("[" + Thread.currentThread().getName() + "] " + "[«] " + request[0].toUpperCase());

                    switch (EconomyCalls.valueOf(request[0].toUpperCase())) {
                        case REQUEST_GETMONEY:
                            this.publish(delivery, replyProps, EconomyCalls.CALLBACK_MONEY.name() + "//" + this.provider.get(request[1]));
                            break;
                        case REQUEST_SETMONEY:
                            this.provider.set(request[1], Double.parseDouble(request[2]));
                            this.publish(delivery, replyProps, EconomyCalls.CALLBACK_NULL.name());
                            break;
                        case REQUEST_ACCOUNTEXISTS:
                            this.publish(delivery, replyProps, EconomyCalls.CALLBACK_ACCOUNTEXISTS.name() + "//" + this.provider.has(request[1]));
                            break;
                        case REQUEST_CREATEACCOUNT:
                            this.provider.create(request[1], Double.parseDouble(request[2]));
                            this.publish(delivery, replyProps, EconomyCalls.CALLBACK_NULL.name());
                            break;
                    }
                };

                this.channel.basicConsume("economy", false, callback, (consumerTag -> {
                }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @SneakyThrows
    private void publish(final Delivery delivery, final AMQP.BasicProperties props, String message) {
        this.channel.basicPublish("", delivery.getProperties().getReplyTo(), props, message.getBytes(StandardCharsets.UTF_8));
        this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        this.server.log("[" + Thread.currentThread().getName() + "] " + "[»] " + message.split("//")[0]);
    }

}
