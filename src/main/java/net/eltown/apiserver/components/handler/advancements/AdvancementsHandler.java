package net.eltown.apiserver.components.handler.advancements;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class AdvancementsHandler {

    private final Server server;
    private final AdvancementsProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public AdvancementsHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new AdvancementsProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (AdvancementsCalls.valueOf(delivery.getKey().toUpperCase())) {

                }
            }, "API/Advancements[Receive]", "api.advancements.receive");
        });

        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                final String[] d = request.getData();
                switch (AdvancementsCalls.valueOf(request.getKey().toUpperCase())) {

                }
            }), "API/Advancements[Callback]", "api.advancements.callback");
        });
    }

}
