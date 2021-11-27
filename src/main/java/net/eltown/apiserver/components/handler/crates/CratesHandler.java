package net.eltown.apiserver.components.handler.crates;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crates.data.CrateReward;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class CratesHandler {
    private final Server server;
    private final CratesProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public CratesHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new CratesProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (CratesCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_ADD_CRATE:
                        this.provider.addCrate(d[1], d[2], Integer.parseInt(d[3]));
                        break;
                    case REQUEST_REMOVE_CRATE:
                        this.provider.removeCrate(d[1], d[2], Integer.parseInt(d[3]));
                        break;
                    case REQUEST_DELETE_REWARD:
                        this.provider.deleteCrateReward(d[1]);
                        break;
                    case REQUEST_UPDATE_REWARD:
                        this.provider.updateCrateReward(d[1], d[2], d[3], Integer.parseInt(d[4]), d[5]);
                        break;
                    case REQUEST_INSERT_REWARD_DATA:
                        this.provider.insertCrateReward(d[1], d[2], d[3], Integer.parseInt(d[4]), d[5]);
                        break;
                    case REQUEST_CREATE_PLAYER_DATA:
                        this.provider.createPlayerData(d[1]);
                        break;
                }
            }, "API/Crates[Receive]", "api.crates.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                final String[] d = request.getData();
                switch (CratesCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GET_REWARD_DATA:
                        final CrateReward crateReward = this.provider.cachedCrateRewards.get(d[1]);
                        if (crateReward != null) {
                            request.answer(CratesCalls.CALLBACK_GET_REWARD_DATA.name(), crateReward.getId(), crateReward.getCrate(), crateReward.getDisplayName(), String.valueOf(crateReward.getChance()), crateReward.getData());
                        } else request.answer(CratesCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_GET_CRATE_REWARDS:
                        final StringBuilder builder = new StringBuilder();

                        this.provider.getCrateRewards(d[1]).forEach(e -> {
                            builder.append(e.getId()).append(">:<").append(e.getCrate()).append(">:<").append(e.getDisplayName()).append(">:<").append(e.getChance()).append(">:<").append(e.getData()).append(">#<");
                        });

                        if (builder.length() == 0) builder.append("null>#<");

                        request.answer(CratesCalls.CALLBACK_GET_CRATE_REWARDS.name(), builder.substring(0, builder.length() - 3));
                        break;
                    case REQUEST_PLAYER_DATA:
                        final StringBuilder builder1 = new StringBuilder();

                        this.provider.cachedCratePlayers.get(d[1]).getData().forEach((k, v) -> {
                            builder1.append(k).append(":").append(v).append("#");
                        });

                        if (builder1.length() == 0) builder1.append("null#");

                        request.answer(CratesCalls.CALLBACK_PLAYER_DATA.name(), builder1.substring(0, builder1.length() - 1));
                        break;
                }
            }, "API/Crates[Callback]", "api.crates.callback");
        });
    }

}
