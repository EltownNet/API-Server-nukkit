package net.eltown.apiserver.components.handler.giftkeys;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.giftkeys.data.Giftkey;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GiftkeyProvider {

    private final MongoClient client;
    private final MongoCollection<Document> giftkeyCollection;
    public final TinyRabbit tinyRabbit;

    public final LinkedHashMap<String, Giftkey> giftkeys = new LinkedHashMap<>();

    @SneakyThrows
    public GiftkeyProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.giftkeyCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("giftkeys");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Giftkeys[Main]");

        server.log("Giftkeys werden in den Cache geladen...");
        for (final Document document : this.giftkeyCollection.find()) {
            this.giftkeys.put(document.getString("_id"), new Giftkey(
                    document.getString("_id"),
                    document.getInteger("maxUses"),
                    document.getList("uses", String.class),
                    document.getList("rewards", String.class),
                    document.getList("marks", String.class)
            ));
        }
        server.log(this.giftkeys.size() + " Giftkeys wurden in den Cache geladen...");
    }

    public void createKey(final int maxUses, final List<String> rewards, List<String> marks, final Consumer<String> keyCallback) {
        final String key = this.createKey(5) + "-" + this.createKey(5);
        this.giftkeys.put(key, new Giftkey(key, maxUses, new ArrayList<>(), rewards, marks));
        CompletableFuture.runAsync(() -> {
            this.giftkeyCollection.insertOne(new Document("_id", key).append("maxUses", maxUses).append("uses", new ArrayList<>()).append("rewards", rewards).append("marks", marks));
        });
        keyCallback.accept(key);
    }

    public boolean keyExists(final String key) {
        return this.giftkeys.containsKey(key);
    }

    public Giftkey getGiftKey(final String key) {
        return this.giftkeys.get(key);
    }

    public void redeemKey(final String key, final String player) {
        final Giftkey giftkey = this.giftkeys.get(key);
        final List<String> list = giftkey.getUses();
        list.add(player);
        giftkey.setUses(list);
        CompletableFuture.runAsync(() -> {
            this.giftkeyCollection.updateOne(new Document("_id", key), new Document("$set", new Document("uses", list)));

            if (list.size() >= giftkey.getMaxUses()) this.deleteKey(key);
        });
    }

    public boolean alreadyRedeemed(final String key, final String player) {
        final AtomicBoolean aBoolean = new AtomicBoolean(false);
        this.giftkeys.values().forEach(e -> {
            if (e.getKey().equals(key)) {
                if (e.getUses().contains(player)) aBoolean.set(true);
            }
        });
        return aBoolean.get();
    }

    public List<String> getKeysByMark(final String mark) {
        final List<String> keyList = new ArrayList<>();
        this.giftkeys.forEach((key, giftkey) -> {
            if (giftkey.getMarks().contains(mark)) {
                if (!giftkey.getUses().contains(mark)) {
                    keyList.add(giftkey.getKey());
                }
            }
        });
        return keyList;
    }

    public void addMarkToKey(final String key, final String target, final String from) {
        try {
            final List<String> list = new ArrayList<>();
            this.giftkeys.get(key).getMarks().forEach(e -> {
                if (!e.equals(from)) {
                    list.add(e);
                }
            });
            list.add(target);
            this.giftkeys.get(key).setMarks(list);

            CompletableFuture.runAsync(() -> {
                this.giftkeyCollection.updateOne(new Document("_id", key), new Document("$set", new Document("marks", list)));
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteKey(final String key) {
        this.giftkeys.remove(key);
        CompletableFuture.runAsync(() -> {
            this.giftkeyCollection.findOneAndDelete(new Document("_id", key));
        });
    }

    private String createKey(final int i) {
        final String chars = "ABCDEFGHJKLMNOPQRSTUVWXYZ";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

}
