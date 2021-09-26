package net.eltown.apiserver.components.handler.advancements;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.advancements.data.Advancement;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class AdvancementsProvider {

    private final MongoClient client;
    private final MongoCollection<Document> advancementsCollection, playerCollection;
    public final TinyRabbit tinyRabbit;

    public final LinkedHashMap<String, Advancement> advancements = new LinkedHashMap<>();
    public final LinkedHashMap<String, List<String>> advancementPlayers = new LinkedHashMap<>();

    @SneakyThrows
    public AdvancementsProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.advancementsCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("advancements_data");
        this.playerCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("advancements_players");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Advancements[Main]");

        server.log("Advancements werden in den Cache geladen...");
        for (final Document document : this.advancementsCollection.find()) {

        }
        server.log(this.advancements.size() + " Advancements wurden in den Cache geladen...");
    }

    public void createAdvancement(final String advancement, final String type, final String data, final String displayName, final String displayDescription, final int requiredProgress) {
        this.advancements.put(advancement, new Advancement(advancement, type, data, displayName, displayDescription, requiredProgress));
        CompletableFuture.runAsync(() -> {
            this.advancementsCollection.insertOne(new Document("_id", advancement)
                    .append("type", type)
                    .append("data", data)
                    .append("displayName", displayName)
                    .append("displayDescription", displayDescription)
                    .append("requiredProgress", requiredProgress)
            );
        });
    }

    public void deleteAdvancement(final String advancement) {
        this.advancements.remove(advancement);
        CompletableFuture.runAsync(() -> this.advancementsCollection.findOneAndDelete(new Document("_id", advancement)));
    }

    public void updatePlayer(final String player, final String advancement, final String progress) {
        final List<String> progressData = this.advancementPlayers.get(player);
        if (progressData == null) {
            final List<String> list = new ArrayList<>(Collections.singletonList(advancement + "#" + progress));
            this.advancementPlayers.put(player, list);


        } else {

        }
    }
}
