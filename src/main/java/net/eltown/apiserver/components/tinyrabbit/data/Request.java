package net.eltown.apiserver.components.tinyrabbit.data;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class Request {

    @Getter
    private final String key;
    @Getter
    private final String[] data;
    private final Channel channel;
    private final Delivery delivery;
    private final AMQP.BasicProperties props;

    @SneakyThrows
    public void answer(final String key, final String... args) {
        final StringBuilder callBuilder = new StringBuilder(key.toLowerCase() + "//");

        for (String arg : args) {
            callBuilder.append(arg).append("//");
        }
        final String call = callBuilder.substring(0, callBuilder.length() - 2);

        this.channel.basicPublish("", delivery.getProperties().getReplyTo(), props, call.getBytes(StandardCharsets.UTF_8));
        this.channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    }
}
