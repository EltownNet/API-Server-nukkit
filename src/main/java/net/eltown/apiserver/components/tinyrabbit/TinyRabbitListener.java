package net.eltown.apiserver.components.tinyrabbit;

import com.rabbitmq.client.*;
import net.eltown.apiserver.components.tinyrabbit.data.Delivery;
import net.eltown.apiserver.components.tinyrabbit.data.Request;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class TinyRabbitListener {

    final ConnectionFactory factory;
    private boolean throwExceptions = false;

    public TinyRabbitListener(final String host) {
        this.factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setAutomaticRecoveryEnabled(true);
    }

    public void throwExceptions(final boolean value) {
        this.throwExceptions = value;
    }

    public void receive(final Consumer<Delivery> received, final String connectionName, final String queue) {
        try {
            final Connection connection = factory.newConnection(connectionName);
            final Channel channel = connection.createChannel();
            channel.queueDeclare(queue, false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    received.accept(new Delivery(message.toUpperCase().split("//")[0], message.split("//")));
                } catch (final Exception ex) {
                    if (this.throwExceptions) ex.printStackTrace();
                }
            };

            if (this.throwExceptions) {
                channel.addShutdownListener(e -> {
                    e.printStackTrace();
                    System.out.println("Warnung: Ein TinyRabbitListener Receive Channel wurde aufgrund eines Fehlers geschlossen.");
                    System.out.println("Der Channel wird neugestartet.");
                    this.receive(received, connectionName, queue);
                });
            }

            channel.basicConsume(queue, true, deliverCallback, consumerTag -> { });
        } catch (final Exception ex) {
            if (this.throwExceptions) ex.printStackTrace();
        }
    }

    public void callback(final Consumer<Request> request, final String connectionName, final String queue) {
        try {
            final Connection connection = factory.newConnection(connectionName);
            final Channel channel = connection.createChannel();
            channel.queueDeclare(queue, false, false, false, null);

            final DeliverCallback callback = (tag, delivery) -> {
                try {
                    final AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                            .Builder()
                            .correlationId(delivery.getProperties().getCorrelationId())
                            .build();

                    final String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    request.accept(new Request(message.toUpperCase().split("//")[0], message.split("//"), channel, delivery, replyProps));
                } catch (final Exception ex) {
                    if (this.throwExceptions) ex.printStackTrace();
                }
            };

            if (this.throwExceptions) {
                channel.addShutdownListener(e -> {
                    e.printStackTrace();
                    System.out.println("Warnung: Ein TinyRabbitListener Callback Channel wurde aufgrund eines Fehlers geschlossen.");
                    System.out.println("Der Channel wird neugestartet.");
                    this.callback(request, connectionName, queue);
                });
            }

            channel.basicConsume(queue, false, callback, (consumerTag -> {
            }));
        } catch (final Exception ex) {
            if (this.throwExceptions) ex.printStackTrace();
        }
    }

}
