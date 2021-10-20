package net.eltown.apiserver.components.handler.level;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.level.data.Level;
import net.eltown.apiserver.components.handler.level.data.LevelReward;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class LevelHandler {

    private final Server server;
    private final LevelProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public LevelHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
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
                    case REQUEST_UPDATE_REWARD:
                        final int l = Integer.parseInt(delivery.getData()[1]);
                        if (this.provider.cachedRewardData.containsKey(l)) {
                            this.provider.updateReward(l, delivery.getData()[2], delivery.getData()[3]);
                        } else this.provider.insertReward(l, delivery.getData()[2], delivery.getData()[3]);
                        break;
                    case REQUEST_REMOVE_REWARD:
                        this.provider.removeReward(Integer.parseInt(delivery.getData()[1]));
                        break;
                }
            }, "API/Level[Receive]", "api.level.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                switch (LevelCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_LEVEL:
                        final Level targetLevel = this.provider.getLevelData(request.getData()[1]);
                        request.answer(LevelCalls.CALLBACK_LEVEL.name(), targetLevel.getPlayer(), String.valueOf(targetLevel.getLevel()), String.valueOf(targetLevel.getExperience()));
                        break;
                    case REQUEST_LEVEL_REWARD:
                        final int level = Integer.parseInt(request.getData()[1]);
                        if (this.provider.cachedRewardData.containsKey(level)) {
                            final LevelReward levelReward = this.provider.cachedRewardData.get(level);
                            request.answer(LevelCalls.CALLBACK_LEVEL_REWARD.name(), String.valueOf(levelReward.getId()), levelReward.getDescription(), levelReward.getData());
                        } else request.answer(LevelCalls.CALLBACK_NULL.name(), "null");
                        break;
                }
            }, "API/Level[Callback]", "api.level.callback");
        });
    }
}
