package net.eltown.apiserver.components.handler.quests;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.quests.data.Quest;
import net.eltown.apiserver.components.handler.quests.data.QuestPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class QuestProvider {

    private final MongoClient client;
    private final MongoCollection<Document> questCollection, questDataCollection;
    public final TinyRabbit tinyRabbit;

    public final HashMap<String, Quest> cachedQuests = new HashMap<>();
    public final HashMap<String, QuestPlayer> cachedQuestPlayer = new HashMap<>();

    @SneakyThrows
    public QuestProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.questCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("quests_quests");
        this.questDataCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("quests_data");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Quests[Main]");

        server.log("Quests werden in den Cache geladen...");
        for (final Document document : this.questCollection.find()) {
            this.cachedQuests.put(document.getString("_id"), new Quest(
                    document.getString("_id"),
                    document.getString("displayName"),
                    document.getString("description"),
                    document.getString("data"),
                    document.getInteger("required"),
                    document.getLong("expire"),
                    document.getString("rewardData"),
                    document.getString("link")
            ));
        }
        server.log(this.cachedQuests.size() + " Quests wurden in den Cache geladen...");

        server.log("QuestDaten werden in den Cache geladen...");
        for (final Document document : this.questDataCollection.find()) {
            final List<String> rawData = document.getList("data", String.class);
            final List<QuestPlayer.QuestPlayerData> questPlayerData = new ArrayList<>();

            for (final String s : rawData) {
                final String[] sSplit = s.split("-:-");
                questPlayerData.add(new QuestPlayer.QuestPlayerData(sSplit[0], Long.parseLong(sSplit[1]), Integer.parseInt(sSplit[2]), Integer.parseInt(sSplit[3])));
            }

            this.cachedQuestPlayer.put(document.getString("_id"), new QuestPlayer(
                    document.getString("_id"),
                    questPlayerData
            ));
        }
        server.log(this.cachedQuestPlayer.size() + " QuestDaten wurden in den Cache geladen...");
    }

    public void createQuest(final String nameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        this.cachedQuests.put(nameId, new Quest(nameId, displayName, description, data, required, expire, rewardData, link));

        CompletableFuture.runAsync(() -> {
            this.questCollection.insertOne(new Document("_id", nameId)
                    .append("displayName", displayName)
                    .append("description", description)
                    .append("data", data)
                    .append("required", required)
                    .append("expire", expire)
                    .append("rewardData", rewardData)
                    .append("link", link)
            );
        });
    }

    public boolean questExists(final String nameId) {
        return this.cachedQuests.containsKey(nameId);
    }

    public void removeQuest(final String nameId) {
        this.cachedQuests.remove(nameId);

        CompletableFuture.runAsync(() -> {
            this.questCollection.findOneAndDelete(new Document("_id", nameId));
        });
    }

    public void updateQuest(final String nameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        this.cachedQuests.remove(nameId);
        this.cachedQuests.put(nameId, new Quest(nameId, displayName, description, data, required, expire, rewardData, link));

        CompletableFuture.runAsync(() -> {
            this.questCollection.updateOne(new Document("_id", nameId), new Document("$set", new Document("displayName", displayName).append("description", description).append("data", data).append("required", required).append("expire", expire).append("rewardData", rewardData).append("link", link)));
        });
    }

    public void createPlayer(final String player) {
        this.cachedQuestPlayer.put(player, new QuestPlayer(player, new ArrayList<>()));

        CompletableFuture.runAsync(() -> {
            this.questDataCollection.insertOne(new Document("_id", player).append("data", new ArrayList<String>()));
        });
    }

    public boolean playerExists(final String player) {
        return this.cachedQuestPlayer.containsKey(player);
    }

    public void setQuestOnPlayer(final String player, final String questNameId) {
        //this.checkIfQuestIsExpired(player);
        final Quest quest = this.cachedQuests.get(questNameId);

        final List<QuestPlayer.QuestPlayerData> playerData = this.cachedQuestPlayer.get(player).getQuestPlayerData();
        playerData.add(new QuestPlayer.QuestPlayerData(quest.getNameId(), (System.currentTimeMillis() + quest.getExpire()), quest.getRequired(), 0));
        this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        CompletableFuture.runAsync(() -> {
            final Document document = this.questDataCollection.find(new Document("_id", player)).first();
            assert document != null;
            final List<String> list = document.getList("data", String.class);
            list.add(quest.getNameId() + "-:-" + (System.currentTimeMillis() + quest.getExpire()) + "-:-" + quest.getRequired() + "-:-" + 0);

            this.questDataCollection.updateOne(new Document("_id", player), new Document("$set", new Document("data", list)));
        });
    }

    public void updateQuestPlayerProgress(final String player, final String questNameId, final int current) {
        //this.checkIfQuestIsExpired(player);
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) {
                if (!(e.getCurrent() >= e.getRequired())) {
                    e.setCurrent(current);

                    CompletableFuture.runAsync(() -> {
                        final Document document = this.questDataCollection.find(new Document("_id", player)).first();
                        assert document != null;
                        final List<String> list = document.getList("data", String.class);
                        list.removeIf(s -> s.startsWith(questNameId));

                        assert questPlayerData != null;
                        list.add(questPlayerData.getQuestNameId() + "-:-" + questPlayerData.getExpire() + "-:-" + questPlayerData.getRequired() + "-:-" + current);

                        this.questDataCollection.updateOne(new Document("_id", player), new Document("$set", new Document("data", list)));
                    });
                }
            }
        });
    }

    public void removeQuestFromPlayer(final String player, final String questNameId) {
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        final List<QuestPlayer.QuestPlayerData> playerData = this.cachedQuestPlayer.get(player).getQuestPlayerData();
        playerData.remove(questPlayerData);
        this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        CompletableFuture.runAsync(() -> {
            final Document document = this.questDataCollection.find(new Document("_id", player)).first();
            assert document != null;
            final List<String> list = document.getList("data", String.class);
            assert questPlayerData != null;
            list.removeIf(s -> s.startsWith(questPlayerData.getQuestNameId()));

            this.questDataCollection.updateOne(new Document("_id", player), new Document("$set", new Document("data", list)));
        });
    }

    private QuestPlayer.QuestPlayerData getQuestPlayerDataFromQuestId(final String player, final String questNameId) {
        //this.checkIfQuestIsExpired(player);
        final AtomicReference<QuestPlayer.QuestPlayerData> questPlayerData = new AtomicReference<>();

        if (this.cachedQuestPlayer.get(player) == null || this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) return null;

        this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) questPlayerData.set(e);
        });

        return questPlayerData.get();
    }

    public Quest getRandomQuestByLink(final String link) {
        final List<Quest> list = new ArrayList<>();
        this.cachedQuests.values().forEach(e -> {
            if (e.getLink().equals(link)) list.add(e);
        });

        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public void checkIfQuestIsExpired(final String player) {
        final List<String> list = new ArrayList<>();
        if (!this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) {
            this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                if (e.getExpire() < System.currentTimeMillis()) list.add(e.getQuestNameId());
            });
        }

        if (!list.isEmpty()) {
            list.forEach(e -> this.removeQuestFromPlayer(player, e));
        }
    }

}
