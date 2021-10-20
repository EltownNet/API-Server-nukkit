package net.eltown.apiserver.components.handler.rewards;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.rewards.data.DailyReward;
import net.eltown.apiserver.components.handler.rewards.data.RewardPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.List;

public class RewardHandler {

    private final Server server;
    private final RewardProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public RewardHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new RewardProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (RewardCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_ADD_REWARD:
                        this.provider.createDailyReward(d[1], Integer.parseInt(d[2]), Integer.parseInt(d[3]), d[4]);
                        break;
                    case REQUEST_REMOVE_REWARD:
                        this.provider.removeDailyReward(d[1]);
                        break;
                    case REQUEST_UPDATE_DAILY_REWARD:
                        final DailyReward dailyReward = new DailyReward(d[1], d[2], Integer.parseInt(d[3]), Integer.parseInt(d[4]), d[5]);
                        this.provider.updateDailyReward(dailyReward);
                        break;
                    case REQUEST_ADD_STREAK:
                        this.provider.addStreak(d[1]);
                        break;
                    case REQUEST_RESET_STREAK:
                        this.provider.resetStreak(d[2]);
                        break;
                }
            }, "API/Rewards[Receive]", "api.rewards.receive");
        });

        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                final String[] d = request.getData();
                switch (RewardCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_PLAYER_DATA:
                        if (!this.provider.playerAccountExists(d[1])) {
                            this.provider.createPlayerAccount(d[1]);
                        }
                        final RewardPlayer rewardPlayer = this.provider.rewardPlayers.get(d[1]);
                        request.answer(RewardCalls.CALLBACK_PLAYER_DATA.name(), rewardPlayer.getPlayer(), String.valueOf(rewardPlayer.getDay()), String.valueOf(rewardPlayer.getLastReward()), String.valueOf(rewardPlayer.getOnlineTime()));
                        break;
                    case REQUEST_REWARDS:
                        final List<DailyReward> rewards = this.provider.getRewardsByDay(Integer.parseInt(d[1]));

                        if (rewards == null || rewards.isEmpty()) {
                            request.answer(RewardCalls.CALLBACK_REWARDS.name(), "null");
                            return;
                        }

                        final StringBuilder builder = new StringBuilder();
                        rewards.forEach(e -> {
                            builder.append(e.getDescription()).append(">:<").append(e.getId()).append(">:<").append(e.getDay()).append(">:<").append(e.getChance()).append(">:<").append(e.getData()).append("-:-");
                        });
                        final String rewardString = builder.substring(0, builder.length() - 3);

                        request.answer(RewardCalls.CALLBACK_REWARDS.name(), rewardString);
                        break;
                }
            }), "API/Rewards[Callback]", "api.rewards.callback");
        });
    }

}
