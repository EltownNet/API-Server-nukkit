package net.eltown.apiserver.components.handler.groupmanager;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.groupmanager.data.Group;
import net.eltown.apiserver.components.handler.groupmanager.data.GroupedPlayer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupProvider {

    private final MongoClient client;
    private final MongoCollection<Document> groupCollection, playerCollection;

    public final HashMap<String, Group> groups = new HashMap<>();
    public final HashMap<String, GroupedPlayer> groupedPlayers = new HashMap<>();

    public GroupProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.groupCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("group_groups");
        this.playerCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("group_players");

        server.log("Gruppen werden in den Cache geladen...");
        for (final Document document : this.groupCollection.find()) {
            this.groups.put(document.getString("group"), new Group(
                    document.getString("group"),
                    document.getList("permissions", String.class),
                    document.getList("inheritances", String.class)
            ));
        }
        server.log(this.groups.size() + " Gruppen wurden in den Cache geladen...");

        server.log("Spieler werden in den Cache geladen...");
        for (final Document document : this.playerCollection.find()) {
            this.groupedPlayers.put(document.getString("player"), new GroupedPlayer(
                    document.getString("player"),
                    document.getString("group"),
                    document.getLong("duration")
            ));
        }
        server.log(this.groupedPlayers.size() + " Spieler wurden in den Cache geladen...");

        if (this.groups.get("SPIELER") == null) {
            this.createGroup("SPIELER");
        }
    }

    public boolean playerExists(final String player) {
        return this.groupedPlayers.containsKey(player);
    }

    public boolean isInGroup(final String player, final String group) {
        return this.groupedPlayers.get(player).getGroup().equals(group);
    }

    public void createPlayer(final String player) {
        this.groupedPlayers.put(player, new GroupedPlayer(player, "SPIELER", -1));
        CompletableFuture.runAsync(() -> {
            this.playerCollection.insertOne(new Document("player", player).append("group", "SPIELER").append("duration", (long) -1));
        });
    }

    public void setGroup(final String player, final String group, final long duration) {
        this.groupedPlayers.get(player).setGroup(group);
        this.groupedPlayers.get(player).setDuration(duration);
        System.out.println(group);
        System.out.println(this.groupedPlayers.get(player).getGroup());
        CompletableFuture.runAsync(() -> {
            this.playerCollection.updateMany(new Document("player", player), new Document("$set", new Document("group", group).append("duration", duration)));
        });
    }

    public void createGroup(final String group) {
        this.groups.put(group, new Group(group, new ArrayList<String>(), new ArrayList<String>()));
        CompletableFuture.runAsync(() -> {
            this.groupCollection.insertOne(new Document("group", group.toUpperCase()).append("permissions", new ArrayList<String>()).append("inheritances", new ArrayList<String>()));
        });
    }

    public void removeGroup(final String group) {
        this.groups.remove(group);
        this.groupedPlayers.values().forEach(e -> {
            if (e.getGroup().equals(group)) {
                this.setGroup(e.getPlayer(), "SPIELER", -1);
            }
        });
        CompletableFuture.runAsync(() -> {
            this.groupCollection.findOneAndDelete(new Document("group", group));
        });
    }

    public void addPermission(final String group, final String permission) {
        final Group sGroup = this.groups.get(group);
        sGroup.getPermissions().add(permission);
        CompletableFuture.runAsync(() -> {
            final Document document = this.groupCollection.find(new Document("group", group)).first();
            assert document != null;
            final List<String> list = document.getList("permissions", String.class);
            list.add(permission);
            this.playerCollection.updateOne(document, new Document("$set", new Document("permissions", list)));
        });
    }

    public void removePermission(final String group, final String permission) {
        final Group sGroup = this.groups.get(group);
        sGroup.getPermissions().remove(permission);
        CompletableFuture.runAsync(() -> {
            final Document document = this.groupCollection.find(new Document("group", group)).first();
            assert document != null;
            final List<String> list = document.getList("permissions", String.class);
            list.remove(permission);
            this.playerCollection.updateOne(document, new Document("$set", new Document("permissions", list)));
        });
    }

}
