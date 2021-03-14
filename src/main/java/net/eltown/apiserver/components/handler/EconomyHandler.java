package net.eltown.apiserver.components.handler;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

public class EconomyHandler extends Handler {

    private final Channel channel;

    @SneakyThrows
    public EconomyHandler(final Connection connection) {
        this.channel = connection.createChannel();
        this.channel.queueDeclare("economy", false, false, false, null);
        this.channel.queuePurge("economy");
        this.channel.basicQos(1);

        this.startListening();
    }

    @Override
    @SneakyThrows
    public void startListening() {
        final DeliverCallback callback = (tag, delivery) -> {
            final AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

            String response = "Eine Nachricht der Proxy!";

            this.channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
            this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        this.channel.basicConsume("economy", false, callback, (consumerTag -> { }));
    }

}
