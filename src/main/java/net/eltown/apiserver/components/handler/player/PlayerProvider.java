package net.eltown.apiserver.components.handler.player;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlayerProvider {

    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final Map<String, SyncPlayer> players = new HashMap<>();

    public PlayerProvider(final Server server) {
        server.log("Spieler werden in den Cache geladen...");
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.collection = this.client.getDatabase(config.getString("MongoDB.PlayerDB")).getCollection("players");

        for (Document document : this.collection.find()) {
            this.players.put(document.getString("_id"),
                    new SyncPlayer(
                            document.getString("inventory"),
                            document.getString("enderchest"),
                            document.getString("health"),
                            document.getString("food"),
                            document.getString("exp"),
                            document.getString("level"),
                            document.getString("effects"),
                            document.getString("gamemode"),
                            true
                    )
            );
        }
        server.log(this.players.size() + " Spieler wurden in den Cache geladen.");
    }

    public SyncPlayer get(String id) {
        return players.getOrDefault(id, new SyncPlayer("empty", "empty", "20.0", "20", "0", "0", "empty", "0", true));
    }

    public void set(String id, SyncPlayer player) {
        this.players.put(id, player);
        CompletableFuture.runAsync(() -> {
            Document document = this.collection.find(new Document("_id", id)).first();
            if (document != null) {
                this.collection.updateOne(new Document("_id", id), new Document("$set", new Document("inventory", player.getInvString())
                        .append("enderchest", player.getEcString())
                        .append("health", player.getHealth())
                        .append("food", player.getFood())
                        .append("exp", player.getExp())
                        .append("level", player.getLevel()))
                        .append("effects", player.getEffects())
                        .append("gamemode", player.getGamemode())
                );
            } else {
                this.collection.insertOne(new Document("_id", id)
                        .append("inventory", player.getInvString())
                        .append("enderchest", player.getEcString())
                        .append("health", player.getHealth())
                        .append("food", player.getFood())
                        .append("exp", player.getExp())
                        .append("level", player.getLevel())
                        .append("effects", player.getEffects())
                        .append("gamemode", player.getGamemode())
                );
            }
        });
    }

}
