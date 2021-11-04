package net.eltown.apiserver.components.handler.shops;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;
import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopProvider {

    private final Server server;
    private final MongoClient client;
    private final MongoCollection<Document> collection;
    @Getter
    private final Map<String, ItemPrice> prices = new HashMap<>();
    @Getter
    private final Set<String> toUpdate = new HashSet<>();

    private final double increaseFactor = 0.05;
    private final int amountFactor = 64;

    public ShopProvider(final Server server) {
        this.server = server;
        server.log("Shop Preise werden in den Cache geladen...");
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.collection = this.client.getDatabase(config.getString("MongoDB.ShopsDB")).getCollection("shop_prices");

        for (final Document e : this.collection.find()) {
            final String[] splitId = e.getString("_id").split(":");

            final int[] id = new int[]{Integer.parseInt(splitId[0]), Integer.parseInt(splitId[1])};
            this.prices.put(this.stringId(id),
                    new ItemPrice(id, e.getDouble("price"), e.getDouble("minBuy"), e.getDouble("minSell"), e.getInteger("bought"), e.getInteger("sold"))
            );
        }

        server.log(this.prices.size() + " Shop Preise wurden in den Cache geladen.");
        final ShopTask task = new ShopTask(server, this);
        task.run();
    }

    public double[] getPrice(final int[] id) {
        if (this.prices.containsKey(this.stringId(id))) {
            final ItemPrice price = this.prices.get(this.stringId(id));
            return new double[]{
                    Math.max(price.getPrice(), price.getMinBuy()),
                    Math.max(price.getPrice() * 0.23, price.getMinSell())
            };
        } else {
            final ItemPrice price = this.createPrice(id);
            return new double[]{
                    price.getPrice(),
                    price.getPrice() * 0.23
            };
        }
    }

    @Deprecated
    public double getSellPrice(final double d) {
        return .23 * d;
    }

    public void setMinBuy(final int[] id, final double minBuy) {
        final ItemPrice ip = this.prices.getOrDefault(stringId(id), this.createPrice(id));
        ip.setMinBuy(minBuy);
        this.updatePrice(ip);
    }

    public void setMinSell(final int[] id, final double minSell) {
        final ItemPrice ip = this.prices.getOrDefault(stringId(id), this.createPrice(id));
        ip.setMinSell(minSell);
        this.updatePrice(ip);
    }

    public void setPrice(final int[] id, final double price) {
        final ItemPrice ip = this.prices.getOrDefault(stringId(id), this.createPrice(id));
        ip.setPrice(price);
        this.updatePrice(ip);
    }

    public void addBought(final int[] id, final int amount) {
        final ItemPrice price = this.prices.get(this.stringId(id));
        price.addBought(amount);
        this.getPrices().put(this.stringId(price.getId()), price);
        this.toUpdate.add(this.stringId(id));
        if (price.getBought() >= amountFactor * 2) this.updatePrices();
    }

    public void addSold(final int[] id, final int amount) {
        final ItemPrice price = this.prices.get(this.stringId(id));
        price.addSold(amount);
        this.getPrices().put(this.stringId(price.getId()), price);
        this.toUpdate.add(this.stringId(id));
        if (price.getSold() >= amountFactor * 2) this.updatePrices();
    }

    public void updatePrices() {
        final AtomicInteger count = new AtomicInteger();
        server.log(4, "Aktualisiere Shop Preise...");

        this.getPrices().values().forEach((e) -> {
            final int toDevide = e.getBought() - e.getSold();
            if (toDevide != 0) {

                final double toMultiply = (double) toDevide / (double) amountFactor;
                double add = 1 + (toMultiply * increaseFactor);

                if (add > 2.0) add = 2.0;
                double newPrice = e.getPrice() * add;
                if (newPrice < (e).getMinSell()) newPrice = e.getMinSell();

                if ((e.getMinSell() * 3) > newPrice) {
                    if (ThreadLocalRandom.current().nextInt(3) == 2) {
                        newPrice += newPrice * (1 + ThreadLocalRandom.current().nextDouble(0.40) + 0.10);
                    }
                }

                e.setPrice(newPrice);
                e.setBought(0);
                e.setSold(0);

                this.toUpdate.add(this.stringId(e.getId()));
                count.incrementAndGet();
            }
        });

        server.log(4, count.get() + " Shop Preise wurden aktualisiert.");
    }

    public void updatePrice(final ItemPrice price) {
        this.prices.put(this.stringId(price.getId()), price);
        CompletableFuture.runAsync(() -> {
            try {
                this.collection.updateOne(new Document("_id", this.stringId(price.getId())),
                        new Document("$set", new Document("price", price.getPrice())
                                .append("bought", price.getBought())
                                .append("sold", price.getSold())
                                .append("minBuy", price.getMinBuy())
                                .append("minSell", price.getMinSell())
                        )
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private ItemPrice createPrice(final int[] id) {
        final ItemPrice itemPrice = new ItemPrice(id, 5, 0.25, 0.01, 0, 0);
        this.getPrices().put(stringId(id), itemPrice);
        CompletableFuture.runAsync(() -> {
            this.collection.insertOne(
                    new Document("_id", this.stringId(id))
                            .append("price", 5d)
                            .append("bought", 0)
                            .append("sold", 0)
            );
        });

        return itemPrice;
    }

    public String stringId(final int[] id) {
        return id[0] + ":" + id[1];
    }

}
