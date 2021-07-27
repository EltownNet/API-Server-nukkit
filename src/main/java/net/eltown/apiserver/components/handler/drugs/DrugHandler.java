package net.eltown.apiserver.components.handler.drugs;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.CryptoProvider;
import net.eltown.apiserver.components.handler.drugs.data.Delivery;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.SplittableRandom;
import java.util.stream.Collectors;

public class DrugHandler {

    private final Server server;
    private final DrugProvider provider;
    private final TinyRabbitListener listener;
    private final SplittableRandom random = new SplittableRandom();

    @SneakyThrows
    public DrugHandler(final Server server) {
        this.server = server;
        this.provider = new DrugProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.listener.throwExceptions(true);
        this.startCallbacking();
        this.startReceiving();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.listener.callback((request) -> {

                switch (DrugCalls.valueOf(request.getKey())) {
                    case GET_DELIVERIES:

                        final StringBuilder sb = new StringBuilder();

                        this.provider.getDeliveries().values().stream().filter(d -> d.getReceiver().equalsIgnoreCase(request.getData()[1])).collect(Collectors.toSet()).forEach((d) -> {
                            sb.append(d.getId())
                                    .append(">>")
                                    .append(d.getReceiver())
                                    .append(">>")
                                    .append(d.getType())
                                    .append(">>")
                                    .append(d.getQuality())
                                    .append(">>")
                                    .append(d.getAmount())
                                    .append(">>")
                                    .append(d.getTime())
                                    .append(">>")
                                    .append(d.getTimeLeft())
                                    .append(">>")
                                    .append(d.isCompleted())
                                    .append("&");
                        });
                        request.answer(DrugCalls.GET_DELIVERIES.name(), sb.length() > 3 ? sb.substring(0, sb.length() - 1) : "null");
                        break;
                }

            }, "API/Drugs/Callback", "drugs.callback");
        });
    }

    public void startReceiving() {
        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery) -> {

                final String[] data = delivery.getData();

                switch (DrugCalls.valueOf(delivery.getKey())) {
                    case ADD_DELIVERY:
                        final String receiver = data[1];
                        final String type = data[2];
                        final String quality = data[3];
                        final int amount = Integer.parseInt(data[4]);
                        final int time = Integer.parseInt(data[5]);

                        this.provider.addDelivery(new Delivery(this.generateID(), receiver, type, quality, amount, time, time, false));
                        break;
                    case REMOVE_DELIVERY:
                        this.provider.removeDelivery(this.provider.getDeliveries().get(delivery.getData()[1]));
                        break;
                }
            }, "API/Drugs/Receive", "drugs.receive");
        });
    }

    private String generateID() {
        return this.provider.getDeliveries().size() + "-" + random.nextInt(9000000) + 1000000 + "-" + random.nextInt(90000) + 10000;
    }

}
