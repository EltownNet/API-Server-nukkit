package net.eltown.apiserver.components.handler.rewards;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.rewards.data.DailyReward;
import net.eltown.apiserver.components.handler.rewards.data.RewardPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RewardProvider {

    private final MongoClient client;
    private final MongoCollection<Document> rewardCollection, playerDataCollection;
    public final TinyRabbit tinyRabbit;

    public final LinkedHashMap<String, DailyReward> dailyRewards = new LinkedHashMap<>();
    public final LinkedHashMap<String, RewardPlayer> rewardPlayers = new LinkedHashMap<>();

    @SneakyThrows
    public RewardProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.rewardCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("reward_rewards");
        this.playerDataCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("reward_playerdata");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Rewards[Main]");

        server.log("DailyRewards werden in den Cache geladen...");
        for (final Document document : this.rewardCollection.find()) {
            this.dailyRewards.put(document.getString("_id"), new DailyReward(
                    document.getString("description"),
                    document.getString("_id"),
                    document.getInteger("day"),
                    document.getInteger("chance"),
                    document.getString("data")
            ));
        }
        server.log(this.dailyRewards.size() + " DailyRewards wurden in den Cache geladen...");

        server.log("RewardDaten werden in den Cache geladen...");
        for (final Document document : this.playerDataCollection.find()) {
            this.rewardPlayers.put(document.getString("_id"), new RewardPlayer(
                    document.getString("_id"),
                    document.getInteger("day"),
                    document.getLong("lastReward"),
                    document.getLong("onlineTime")
            ));
        }
        server.log(this.rewardPlayers.size() + " RewardDaten wurden in den Cache geladen...");
    }

    public void createDailyReward(final String description, final int day, final int chance, final String data) {
        final String id = this.createId(10);
        this.dailyRewards.put(id, new DailyReward(description, id, day, chance, data));

        CompletableFuture.runAsync(() -> {
            this.rewardCollection.insertOne(new Document("_id", id).append("description", description).append("day", day).append("chance", chance).append("data", data));
        });
    }

    public void removeDailyReward(final String id) {
        this.dailyRewards.remove(id);

        CompletableFuture.runAsync(() -> {
            this.rewardCollection.findOneAndDelete(new Document("_id", id));
        });
    }

    public boolean playerAccountExists(final String player) {
        return this.rewardPlayers.containsKey(player);
    }

    public void createPlayerAccount(final String player) {
        this.rewardPlayers.put(player, new RewardPlayer(player, 0, 0, 0));

        CompletableFuture.runAsync(() -> {
            this.playerDataCollection.insertOne(new Document("_id", player).append("day", 0).append("lastReward", (long) 0).append("onlineTime", (long) 0));
        });
    }

    public List<DailyReward> getRewardsByDay(final int day) {
        final List<DailyReward> list = new ArrayList<>();
        this.dailyRewards.values().forEach(e -> {
            if (e.getDay() == day) list.add(e);
        });
        return list;
    }

    public void updateDailyReward(final DailyReward dailyReward) {
        final String id = dailyReward.getId();
        this.dailyRewards.replace(id, this.dailyRewards.get(id), dailyReward);

        CompletableFuture.runAsync(() -> {
            this.rewardCollection.updateOne(new Document("_id", id), new Document("$set", new Document("description", dailyReward.getDescription()).append("chance", dailyReward.getChance()).append("data", dailyReward.getData())));
        });
    }

    public void addStreak(final String player) {
        final RewardPlayer rewardPlayer = this.rewardPlayers.get(player);
        rewardPlayer.setDay(rewardPlayer.getDay() + 1);
        rewardPlayer.setLastReward(System.currentTimeMillis());
        rewardPlayer.setOnlineTime(0);

        CompletableFuture.runAsync(() -> {
            this.playerDataCollection.updateOne(new Document("_id", player), new Document("$set", new Document("day", rewardPlayer.getDay()).append("lastReward", rewardPlayer.getLastReward()).append("onlineTime", rewardPlayer.getOnlineTime())));
        });
    }

    public void resetStreak(final String player) {
        final RewardPlayer rewardPlayer = this.rewardPlayers.get(player);
        rewardPlayer.setDay(0);
        rewardPlayer.setLastReward(0);
        rewardPlayer.setOnlineTime(0);

        CompletableFuture.runAsync(() -> {
            this.playerDataCollection.updateOne(new Document("_id", player), new Document("$set", new Document("day", rewardPlayer.getDay()).append("lastReward", rewardPlayer.getLastReward()).append("onlineTime", rewardPlayer.getOnlineTime())));
        });
    }

    private String createId(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

}
