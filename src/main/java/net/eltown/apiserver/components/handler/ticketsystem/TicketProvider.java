package net.eltown.apiserver.components.handler.ticketsystem;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.ticketsystem.data.Ticket;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TicketProvider {

    private final MongoClient client;
    private final MongoCollection<Document> ticketCollection;
    public final TinyRabbit tinyRabbit;

    public final LinkedHashMap<String, Ticket> tickets = new LinkedHashMap<>();

    @SneakyThrows
    public TicketProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.ticketCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("tickets");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Ticketsystem/Message");

        server.log("Tickets werden in den Cache geladen...");
        for (final Document document : this.ticketCollection.find()) {
            this.tickets.put(document.getString("_id"), new Ticket(
                    document.getString("creator"),
                    document.getString("supporter"),
                    document.getString("_id"),
                    document.getString("subject"),
                    document.getString("section"),
                    document.getString("priority"),
                    document.getList("messages", String.class),
                    document.getString("dateOpened"),
                    document.getString("dateClosed")
            ));
        }
        server.log(this.tickets.size() + " Tickets wurden in den Cache geladen...");
    }

    public void createTicket(final String creator, final String subject, final String section, final String priority, final String message) {
        final String id = this.createId(7, "T");
        final List<String> messages = new ArrayList<>();
        messages.add(creator + ">:<" + message);

        CompletableFuture.runAsync(() -> {
            final Document document = new Document("_id", id)
                    .append("creator", creator)
                    .append("supporter", "null")
                    .append("subject", subject)
                    .append("section", section)
                    .append("priority", priority)
                    .append("messages", messages)
                    .append("dateOpened", this.getDate())
                    .append("dateClosed", "null");
            this.ticketCollection.insertOne(document);
        });

        this.tickets.put(id, new Ticket(creator, "null", id, subject, section, priority, messages, this.getDate(), "null"));
    }

    public void getTickets(final String player, final Consumer<Set<Ticket>> tickets) {
        final LinkedHashSet<Ticket> set = new LinkedHashSet<>();
        this.tickets.values().forEach(e -> {
            if (e.getCreator().equals(player)) {
                set.add(e);
            }
        });
        tickets.accept(set);
    }

    public LinkedHashSet<Ticket> getOpenTickets() {
        final LinkedHashSet<Ticket> set = new LinkedHashSet<>();
        this.tickets.values().forEach(e -> {
            if (e.getSupporter().equals("null") && e.getDateClosed().equals("null")) {
                set.add(e);
            }
        });
        return set;
    }

    public LinkedHashSet<Ticket> getMySupportTickets(final String supporter) {
        final LinkedHashSet<Ticket> set = new LinkedHashSet<>();
        this.tickets.values().forEach(e -> {
            if (e.getSupporter().equals(supporter) && e.getDateClosed().equals("null")) {
                set.add(e);
            }
        });
        return set;
    }

    public Ticket getTicket(final String id) {
        return this.tickets.get(id);
    }

    public void setTicketSupporter(final String ticketId, final String supporter) {
        this.tickets.get(ticketId).setSupporter(supporter);
        CompletableFuture.runAsync(() -> {
            this.ticketCollection.updateOne(new Document("_id", ticketId), new Document("$set", new Document("supporter", supporter)));
        });
    }

    public void setTicketPriority(final String ticketId, final String priority) {
        this.tickets.get(ticketId).setPriority(priority);
        CompletableFuture.runAsync(() -> {
            this.ticketCollection.updateOne(new Document("_id", ticketId), new Document("$set", new Document("priority", priority)));
        });
    }

    public void closeTicket(final String ticketId) {
        this.tickets.get(ticketId).setDateClosed(this.getDate());
        CompletableFuture.runAsync(() -> {
            this.ticketCollection.updateOne(new Document("_id", ticketId), new Document("$set", new Document("dateClosed", this.getDate())));
        });
    }

    public void addNewTicketMessage(final String ticketId, final String sender, final String message) {
        CompletableFuture.runAsync(() -> {
            final Document document = this.ticketCollection.find(new Document("_id", ticketId)).first();
            if (document != null) {
                final List<String> list = document.getList("messages", String.class);
                list.add(sender + ">:<" + message);
                this.ticketCollection.updateOne(new Document("_id", ticketId), new Document("$set", new Document("messages", list)));
                this.tickets.get(ticketId).setMessages(list);
            }
        });
    }

    private String createId(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    private String createId(final int i, final String prefix) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder(prefix + "-");
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    private String getDate() {
        Date now = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        return dateFormat.format(now);
    }

}
