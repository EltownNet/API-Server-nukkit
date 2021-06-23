package net.eltown.apiserver.components.handler.crypto;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CryptoProvider {

    private final Map<String, Wallet> wallets = new HashMap<>();
    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public CryptoProvider(final Server server) {
        server.log("Wallets werden in den Cache geladen...");
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.collection = this.client.getDatabase(config.getString("MongoDB.CryptoDB")).getCollection("wallets");

        for (final Document document : this.collection.find()) {
            this.wallets.put(document.getString("_id"),
                    new Wallet(
                            document.getString("_id"),
                            document.getDouble("CTC"),
                            document.getDouble("ELT"),
                            document.getDouble("NOT")
                    )
            );
        }

        server.log(this.wallets.size() + " Wallets geladen.");
    }

    public Wallet getWallet(final String owner) {
        if (this.wallets.containsKey(owner)) {
            return this.wallets.get(owner);
        } else {
            final Wallet wallet = new Wallet(owner, 0f, 0f, 0f);
            this.createWallet(wallet);
            return this.getWallet(owner);
        }
    }

    public void updateWallet(final Wallet wallet) {
        this.wallets.put(wallet.getOwner(), wallet);
        CompletableFuture.runAsync(() -> {
            this.collection.updateOne(new Document("_id", wallet.getOwner()),
                    new Document("$set", new Document("CTC", wallet.getCtc())
                            .append("ELT", wallet.getElt())
                            .append("NOT", wallet.getNot()))
            );
        });
    }

    private void createWallet(final Wallet wallet) {
        this.wallets.put(wallet.getOwner(), wallet);
        CompletableFuture.runAsync(() -> {
            this.collection.insertOne(new Document("_id", wallet.getOwner())
                    .append("CTC", wallet.getCtc())
                    .append("ELT", wallet.getElt())
                    .append("NOT", wallet.getNot())
            );
        });
    }

}
