package net.eltown.apiserver.components.handler.association;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class AssociationHandler {

    private final Server server;
    private final AssociationProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public AssociationHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new AssociationProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (AssociationCalls.valueOf(delivery.getKey().toUpperCase())) {

                }
            }, "API/Associations[Receive]", "api.associations.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                final String[] d = request.getData();
                switch (AssociationCalls.valueOf(request.getKey().toUpperCase())) {

                }
            }, "API/Associations[Callback]", "api.associations.callback");
        });
    }

}
