package net.eltown.apiserver.components.handler.drugs;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.CryptoProvider;
import net.eltown.apiserver.components.handler.drugs.data.Delivery;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class DrugHandler {

    private final Server server;
    private final CryptoProvider provider;
    private final TinyRabbitListener listener;

    @SneakyThrows
    public DrugHandler(final Server server) {
        this.server = server;
        this.provider = new CryptoProvider(server);
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

                        break;
                }

            }, "API/Drugs/Callback", "drugs.callback");
        });
    }

    public void startReceiving() {
        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery) -> {
                switch (DrugCalls.valueOf(delivery.getKey())) {
                    case ADD_DELIVERY:

                        break;
                    case REMOVE_DELIVERY:

                        break;
                }
            }, "API/Drugs/Receive", "drugs.receive");
        });
    }

}
