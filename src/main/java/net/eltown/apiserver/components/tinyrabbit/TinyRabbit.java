package net.eltown.apiserver.components.tinyrabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.eltown.apiserver.components.tinyrabbit.data.Delivery;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class TinyRabbit {

    private Connection connection;
    private Channel channel;
    private final String host, connectionName;
    private boolean throwExceptions = false;

    public TinyRabbit(final String host, final String connectionName) throws Exception {
        this.host = host;
        this.connectionName = connectionName;

        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setAutomaticRecoveryEnabled(true);
        this.connection = factory.newConnection(connectionName);

        this.channel = connection.createChannel();

        if (throwExceptions) {
            this.channel.addShutdownListener((e) -> {
                try {
                    e.printStackTrace();
                    System.out.println("Warnung: Ein TinyRabbit Channel wurde aufgrund eines Fehlers geschlossen.");
                    System.out.println("Der Channel wird neugestartet.");
                    this.channel = connection.createChannel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    public void throwExceptions(final boolean value) {
        this.throwExceptions = value;
    }

    public void sendAndReceive(final Consumer<Delivery> received, final String channel, final String key, final String... args) {
        try {
            this.testConnection();

            final String corrId = UUID.randomUUID().toString();
            final StringBuilder callBuilder = new StringBuilder(key.toLowerCase() + "//");
            for (String arg : args) {
                callBuilder.append(arg).append("//");
            }

            final String call = callBuilder.substring(0, callBuilder.length() - 2);
            final String replyQueueName = this.channel.queueDeclare().getQueue();
            final AMQP.BasicProperties properties = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            this.channel.basicPublish("", channel, properties, call.getBytes(StandardCharsets.UTF_8));

            final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

            final String tag = this.channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
                try {
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                    }
                } catch (Exception ex) {
                    if (this.throwExceptions) ex.printStackTrace();
                }
            }, consumerTag -> {
            });

            final String result = response.take();
            this.channel.basicCancel(tag);
            received.accept(new Delivery(result.toUpperCase().split("//")[0], result.split("//")));
        } catch (final Exception ex) {
            if (this.throwExceptions) ex.printStackTrace();
        }
    }

    public void send(final String channel, final String key, final String... args) {
        try {
            this.testConnection();

            final StringBuilder callBuilder = new StringBuilder(key.toLowerCase() + "//");
            for (String arg : args) {
                callBuilder.append(arg).append("//");
            }
            final String call = callBuilder.substring(0, callBuilder.length() - 2);
            this.channel.basicPublish("", channel, null, call.getBytes(StandardCharsets.UTF_8));
        } catch (final Exception ex) {
            if (this.throwExceptions) ex.printStackTrace();
        }
    }

    public void testConnection() {
        try {
            if (!this.channel.isOpen() || !this.connection.isOpen()) {
                final ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(host);
                this.connection = factory.newConnection(this.connectionName);
            }
        } catch (final Exception ex) {
            if (this.throwExceptions) ex.printStackTrace();
        }
    }

}
