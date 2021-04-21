package net.eltown.apiserver.components.handler.economy;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EconomyProvider {

    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final Map<String, Double> players = new HashMap<>();

    public EconomyProvider(final Server server) {
        server.log("Spieler werden in den Cache geladen...");
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.collection = this.client.getDatabase(config.getString("MongoDB.EconomyDB")).getCollection("economy");

        for (Document document : this.collection.find()) {
            this.players.put(document.getString("_id"), document.getDouble("money"));
        }
        server.log(this.players.size() + " Spieler wurden in den Cache geladen.");
    }

    public double get(String id) {
        return players.getOrDefault(id, 0.0);
    }

    public boolean has(String id) {
        return this.players.containsKey(id);
    }

    public void create(String id, double money) {
        this.players.put(id, money);
        CompletableFuture.runAsync(() -> {
            this.collection.insertOne(new Document("_id", id).append("money", money));
        });
    }

    public void set(String id, double money) {
        this.players.put(id, money);
        CompletableFuture.runAsync(() -> {
            this.collection.updateOne(new Document("_id", id), new Document("$set", new Document("money", money)));
        });
    }

    public List<String> getAll() {
        final List<String> list = new ArrayList<>();
        players.forEach((user, money) -> list.add(user + ":" + money));
        return list;
    }

}
