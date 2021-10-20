package net.eltown.apiserver.components.handler.level;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.level.data.Level;
import net.eltown.apiserver.components.handler.level.data.LevelReward;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class LevelProvider {

    private final MongoClient client;
    private final MongoCollection<Document> levelCollection, rewardCollection;
    public final TinyRabbit tinyRabbit;

    private final HashMap<String, Level> cachedData = new HashMap<>();
    public final HashMap<Integer, LevelReward> cachedRewardData = new HashMap<>();

    @SneakyThrows
    public LevelProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.levelCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("player_level");
        this.rewardCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("level_rewards");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Level[Main]");

        server.log("Leveldaten werden in den Cache geladen...");
        for (final Document document : this.levelCollection.find()) {
            this.cachedData.put(document.getString("player"), new Level(
                    document.getString("player"),
                    document.getInteger("level"),
                    document.getDouble("experience")
            ));
        }

        for (final Document document : this.rewardCollection.find()) {
            this.cachedRewardData.put(document.getInteger("_id"), new LevelReward(
                    document.getInteger("_id"),
                    document.getString("description"),
                    document.getString("data")
            ));
        }
        server.log(this.cachedData.size() + " Leveldaten wurden in den Cache geladen.");
    }

    public void createPlayer(final String player) {
        this.cachedData.put(player, new Level(player, 0, 0.0d));

        CompletableFuture.runAsync(() -> {
            this.levelCollection.insertOne(new Document("player", player).append("level", 0).append("experience", 0.0d));
        });
    }

    public boolean playerExists(final String player) {
        return this.cachedData.containsKey(player);
    }

    public Level getLevelData(final String player) {
        if (!this.cachedData.containsKey(player)) this.createPlayer(player);
        return this.cachedData.get(player);
    }

    public void updateToDatabase(final Level level) {
        this.cachedData.put(level.getPlayer(), level);

        CompletableFuture.runAsync(() -> {
            this.levelCollection.updateOne(new Document("player", level.getPlayer()),
                    new Document("$set", new Document("level", level.getLevel()).append("experience", level.getExperience())));
        });
    }

    public void insertReward(final int level, final String description, final String data) {
        this.cachedRewardData.put(level, new LevelReward(level, description, data));

        CompletableFuture.runAsync(() -> {
            this.rewardCollection.insertOne(new Document("_id", level).append("description", description).append("data", data));
        });
    }

    public void updateReward(final int level, final String description, final String data) {
        this.cachedRewardData.remove(level);
        this.cachedRewardData.put(level, new LevelReward(level, description, data));

        CompletableFuture.runAsync(() -> {
            this.rewardCollection.updateOne(new Document("_id", level), new Document("$set", new Document("description", description).append("data", data)));
        });
    }

    public void removeReward(final int level) {
        this.cachedRewardData.remove(level);

        CompletableFuture.runAsync(() -> {
           this.rewardCollection.findOneAndDelete(new Document("_id", level));
        });
    }

}
