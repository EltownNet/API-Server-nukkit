package net.eltown.apiserver.components.handler.shops;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.player.data.SyncPlayer;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;
import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ShopProvider {

    private final MongoClient client;
    private final MongoCollection<Document> collection;
    @Getter
    private final Map<String, ItemPrice> prices = new HashMap<>();
    @Getter
    private final Set<String> toUpdate = new HashSet<>();

    public ShopProvider(final Server server) {
        server.log("Shop Preise werden in den Cache geladen...");
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.collection = this.client.getDatabase(config.getString("MongoDB.ShopDB")).getCollection("prices");

        for (final Document e : this.collection.find()) {
            final String[] splitId = e.getString("_id").split(":");

            final int[] id = new int[]{Integer.parseInt(splitId[0]), Integer.parseInt(splitId[1])};
            this.prices.put(this.stringId(id),
                    new ItemPrice(id, e.getDouble("price"), e.getInteger("bought"), e.getInteger("sold"))
            );
        }

        server.log(this.prices.size() + " Shop Preise wurden in den Cache geladen.");
        final ShopTask task = new ShopTask(server, this);
        task.run();
    }

    public double getPrice(final int[] id) {
        return this.prices.getOrDefault(this.stringId(id), this.createPrice(id)).getPrice();
    }

    public void setPrice(final int[] id, final double price) {
        final ItemPrice ip = this.prices.getOrDefault(stringId(id), this.createPrice(id));
        ip.setPrice(price);
        this.updatePrice(ip);
    }

    public void addBought(final int[] id, final int amount) {
        this.prices.get(this.stringId(id)).addBought(amount);
        this.toUpdate.add(this.stringId(id));
    }

    public void addSold(final int[] id, final int amount) {
        this.prices.get(this.stringId(id)).addSold(amount);
        this.toUpdate.add(this.stringId(id));
    }

    public void updatePrice(final ItemPrice price) {
        CompletableFuture.runAsync(() -> {
            this.collection.updateOne(new Document("_id", this.stringId(price.getId())),
                    new Document("$set", new Document("price", price.getPrice())
                            .append("bought", price.getBought())
                            .append("sold", price.getSold()))
            );
        });
    }

    private ItemPrice createPrice(final int[] id) {
        CompletableFuture.runAsync(() -> {
            this.collection.insertOne(
                    new Document("_id", this.stringId(id))
                            .append("price", 5d)
                            .append("bought", 0)
                            .append("sold", 0)
            );
        });

        return new ItemPrice(id, 5, 0, 0);
    }

    public String stringId(final int[] id) {
        return id[0] + ":" + id[1];
    }

}
