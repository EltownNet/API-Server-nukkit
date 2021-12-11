package net.eltown.apiserver.components.handler.association;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.association.data.Association;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.util.HashMap;

public class AssociationProvider {

    private final MongoClient client;
    private final MongoCollection<Document> associationCollection;
    public final TinyRabbit tinyRabbit;

    public final HashMap<String, Association> cachedAssociations = new HashMap<>();

    @SneakyThrows
    public AssociationProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.associationCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("associations");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Associations[Main]");

        server.log("Vereine werden in den Cache geladen...");
        for (final Document document : this.associationCollection.find()) {

        }
        server.log(this.cachedAssociations.size() + " Vereine wurden in den Cache geladen...");
    }

}
