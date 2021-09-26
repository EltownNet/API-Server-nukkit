package net.eltown.apiserver.components.handler.groupmanager;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.groupmanager.data.Group;
import net.eltown.apiserver.components.handler.groupmanager.data.GroupedPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class GroupProvider {

    private final MongoClient client;
    private final MongoCollection<Document> groupCollection, playerCollection;
    private final TinyRabbit tinyRabbit;

    public final HashMap<String, Group> groups = new HashMap<>();
    public final HashMap<String, GroupedPlayer> groupedPlayers = new HashMap<>();

    @SneakyThrows
    public GroupProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.groupCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("group_groups");
        this.playerCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("group_players");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Groupmanager[Main]");

        server.log("Gruppen werden in den Cache geladen...");
        for (final Document document : this.groupCollection.find()) {
            this.groups.put(document.getString("group"), new Group(
                    document.getString("group"),
                    document.getString("prefix"),
                    document.getList("permissions", String.class),
                    document.getList("inheritances", String.class)
            ));
        }
        server.log(this.groups.size() + " Gruppen wurden in den Cache geladen...");

        server.log("Spieler werden in den Cache geladen...");
        for (final Document document : this.playerCollection.find()) {
            this.groupedPlayers.put(document.getString("_id"), new GroupedPlayer(
                    document.getString("_id"),
                    document.getString("group"),
                    document.getLong("duration"),
                    document.getList("permissions", String.class)
            ));
        }
        server.log(this.groupedPlayers.size() + " Spieler wurden in den Cache geladen...");

        if (this.groups.get("SPIELER") == null) {
            this.createGroup("SPIELER", "§7Spieler §8| §7%p");
        }
    }

    public boolean playerExists(final String player) {
        return this.groupedPlayers.containsKey(player);
    }

    public boolean isInGroup(final String player, final String group) {
        return this.groupedPlayers.get(player).getGroup().equals(group);
    }

    public void createPlayer(final String player) {
        this.groupedPlayers.put(player, new GroupedPlayer(player, "SPIELER", -1, Collections.singletonList("none")));
        CompletableFuture.runAsync(() -> {
            this.playerCollection.insertOne(new Document("_id", player).append("group", "SPIELER").append("duration", (long) -1).append("permissions", Collections.singletonList("none")));
        });
    }

    public void setGroup(final String player, final String group, final long duration) {
        this.groupedPlayers.get(player).setGroup(group);
        this.groupedPlayers.get(player).setDuration(duration);
        System.out.println(group);
        System.out.println(this.groupedPlayers.get(player).getGroup());
        CompletableFuture.runAsync(() -> {
            this.playerCollection.updateMany(new Document("_id", player), new Document("$set", new Document("group", group).append("duration", duration)));
        });
    }

    public void createGroup(final String group, final String prefix) {
        this.groups.put(group, new Group(group, prefix, new ArrayList<String>(), new ArrayList<String>()));
        this.getTinyRabbit().send("core.proxy.groupmanager.receive", GroupCalls.REQUEST_CHANGE_PREFIX.name(), group, prefix);
        CompletableFuture.runAsync(() -> {
            this.groupCollection.insertOne(new Document("group", group.toUpperCase()).append("prefix", prefix).append("permissions", new ArrayList<String>()).append("inheritances", new ArrayList<String>()));
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
            this.groupCollection.updateOne(new Document("group", group), new Document("$set", new Document("permissions", list)));
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
            this.groupCollection.updateOne(new Document("group", group), new Document("$set", new Document("permissions", list)));
        });
    }

    public void addPlayerPermission(final String player, final String permission) {
        final GroupedPlayer sGroup = this.groupedPlayers.get(player);
        sGroup.getPermissions().add(permission);
        CompletableFuture.runAsync(() -> {
            final Document document = this.playerCollection.find(new Document("_id", player)).first();
            assert document != null;
            final List<String> list = document.getList("permissions", String.class);
            list.add(permission);
            this.playerCollection.updateOne(new Document("_id", player), new Document("$set", new Document("permissions", list)));
        });
    }

    public void removePlayerPermission(final String player, final String permission) {
        final GroupedPlayer sGroup = this.groupedPlayers.get(player);
        sGroup.getPermissions().remove(permission);
        CompletableFuture.runAsync(() -> {
            final Document document = this.playerCollection.find(new Document("_id", player)).first();
            assert document != null;
            final List<String> list = document.getList("permissions", String.class);
            list.remove(permission);
            this.playerCollection.updateOne(new Document("_id", player), new Document("$set", new Document("permissions", list)));
        });
    }

    public void addInheritance(final String group, final String inheritance) {
        final Group sGroup = this.groups.get(group);
        sGroup.getInheritances().add(inheritance);
        CompletableFuture.runAsync(() -> {
            final Document document = this.groupCollection.find(new Document("group", group)).first();
            assert document != null;
            final List<String> list = document.getList("inheritances", String.class);
            list.add(inheritance);
            this.groupCollection.updateOne(new Document("group", group), new Document("$set", new Document("inheritances", list)));
        });
    }

    public void removeInheritance(final String group, final String inheritance) {
        final Group sGroup = this.groups.get(group);
        sGroup.getInheritances().remove(inheritance);
        CompletableFuture.runAsync(() -> {
            final Document document = this.groupCollection.find(new Document("group", group)).first();
            assert document != null;
            final List<String> list = document.getList("inheritances", String.class);
            list.remove(inheritance);
            this.groupCollection.updateOne(new Document("group", group), new Document("$set", new Document("inheritances", list)));
        });
    }

    public void changePrefix(final String group, final String prefix) {
        final Group sGroup = this.groups.get(group);
        sGroup.setPrefix(prefix);
        CompletableFuture.runAsync(() -> {
            final Document document = this.groupCollection.find(new Document("group", group)).first();
            assert document != null;
            this.groupCollection.updateOne(new Document("group", group), new Document("$set", new Document("prefix", prefix)));
        });

        this.getTinyRabbit().send("core.proxy.groupmanager.receive", GroupCalls.REQUEST_CHANGE_PREFIX.name(), group, prefix);
    }

}
