package net.eltown.apiserver.components.handler.drugs;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.drugs.data.Delivery;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DrugProvider {

    @Getter
    private final Map<String, Delivery> deliveries = new HashMap<>();
    private final MongoClient client;
    private final MongoCollection<Document> deliveryCollection;

    public DrugProvider(final Server server) {
        server.log("Lieferungen werden in den Cache geladen...");

        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.deliveryCollection = this.client.getDatabase(config.getString("MongoDB.CryptoDB")).getCollection("drug_deliveries");

        for (final Document doc : this.deliveryCollection.find()) {
            this.deliveries.put(doc.getString("_id"),
                    new Delivery(
                            doc.getString("_id"),
                            doc.getString("type"),
                            doc.getString("quality"),
                            doc.getInteger("time"),
                            doc.getInteger("timeLeft"),
                            doc.getBoolean("completed")
                    )
            );
        }

    }

    public void updateDelivery(final Delivery delivery) {
        this.deliveries.put(delivery.getId(), delivery);
        CompletableFuture.runAsync(() -> {
            this.deliveryCollection.updateOne(new Document("_id", delivery.getId()),
                    new Document("$set", new Document("type", delivery.getType())
                            .append("quality", delivery.getQuality())
                            .append("time", delivery.getTime())
                            .append("timeLeft", delivery.getTimeLeft())
                            .append("completed", delivery.isCompleted())
                    )
            );
        });
    }

    public void addDelivery(final Delivery delivery) {
        this.deliveries.put(delivery.getId(), delivery);
        CompletableFuture.runAsync(() -> {
            this.deliveryCollection.insertOne(
                    new Document("_id", delivery.getId())
                            .append("type", delivery.getType())
                            .append("quality", delivery.getQuality())
                            .append("time", delivery.getTime())
                            .append("timeLeft", delivery.getTimeLeft())
                            .append("completed", delivery.isCompleted())
            );
        });
    }

    public void removeDelivery(final Delivery delivery) {
        this.deliveries.remove(delivery.getId());
        CompletableFuture.runAsync(() -> {
            this.deliveryCollection.deleteOne(new Document("_id", delivery.getId()));
        });
    }

}
