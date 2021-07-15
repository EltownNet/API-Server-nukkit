package net.eltown.apiserver.components.handler.level;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.level.data.Level;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class LevelHandler {

    private final Server server;
    private final LevelProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public LevelHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new LevelProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_UPDATE_TO_DATABASE:
                        final Level level = this.provider.getLevelData(delivery.getData()[1]);
                        final Level aLevel = new Level(delivery.getData()[1], Integer.parseInt(delivery.getData()[2]), Double.parseDouble(delivery.getData()[3]));
                        if (aLevel.getExperience() > level.getExperience()) {
                            this.provider.updateToDatabase(aLevel);
                        }
                        break;
                }
            }, "API/Level/Receive", "level.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                switch (LevelCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_LEVEL:
                        final Level targetLevel = this.provider.getLevelData(request.getData()[1]);
                        request.answer(LevelCalls.CALLBACK_LEVEL.name(), targetLevel.getPlayer(), String.valueOf(targetLevel.getLevel()), String.valueOf(targetLevel.getExperience()));
                        break;
                }
            }, "API/Level/Callback", "level.callback");
        });
    }
}
