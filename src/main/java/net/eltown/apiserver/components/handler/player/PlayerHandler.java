package net.eltown.apiserver.components.handler.player;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.Handler;

import java.nio.charset.StandardCharsets;

public class PlayerHandler extends Handler {

    private final Channel channel;

    @SneakyThrows
    public PlayerHandler(final Server server, final Connection connection) {
        this.channel = connection.createChannel();
        this.channel.queueDeclare("playersync", false, false, false, null);
        this.channel.queuePurge("playersync");
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

        this.channel.basicConsume("playersync", false, callback, (consumerTag -> { }));
    }

}
