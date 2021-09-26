package net.eltown.apiserver.components.handler.bank;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.handler.bank.data.BankAccount;
import net.eltown.apiserver.components.handler.bank.data.BankLog;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbit;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@AllArgsConstructor
public class BankProvider {

    private final MongoClient client;
    private final MongoCollection<Document> bankCollection;
    public final TinyRabbit tinyRabbit;

    public final LinkedHashMap<String, BankAccount> bankAccounts = new LinkedHashMap<>();

    @SneakyThrows
    public BankProvider(final Server server) {
        final Config config = server.getConfig();
        this.client = new MongoClient(new MongoClientURI(config.getString("MongoDB.Uri")));
        this.bankCollection = this.client.getDatabase(config.getString("MongoDB.GroupDB")).getCollection("bank_accounts");

        this.tinyRabbit = new TinyRabbit("localhost", "API/Bank[Main]");

        server.log("Bankkonten werden in den Cache geladen...");
        for (final Document document : this.bankCollection.find()) {
            final List<String> dLogs = document.getList("logs", String.class);
            final List<BankLog> bankLogs = new ArrayList<>();

            dLogs.forEach(e -> {
                final String[] rawLog = e.split(";-;");
                bankLogs.add(new BankLog(rawLog[0], rawLog[1], rawLog[2], rawLog[3]));
            });


            this.bankAccounts.put(document.getString("_id"), new BankAccount(
                    document.getString("_id"),
                    document.getString("displayName"),
                    document.getString("owner"),
                    document.getString("password"),
                    document.getDouble("balance"),
                    bankLogs
            ));
        }
        server.log(this.bankAccounts.size() + " Bankkonten wurden in den Cache geladen...");
    }

    public void createBankAccount(final String owner, final String prefix, final BiConsumer<String, String> callbackPassword) {
        final String account = prefix + "-" + this.createKey(7) + "-" + this.createNumberKey(2);
        final String password = this.createNumberKey(4);
        this.bankAccounts.put(account, new BankAccount(account, account, owner, password, 0.0d, new ArrayList<>()));

        CompletableFuture.runAsync(() -> {
            this.bankCollection.insertOne(new Document("_id", account)
                    .append("displayName", account)
                    .append("owner", owner)
                    .append("password", password)
                    .append("balance", 0.0d)
                    .append("logs", new ArrayList<String>())
            );
        });

        this.insertBankLog(account, "Account erstellt.", "§7Das Konto wurde von " + owner + " erstellt.");
        callbackPassword.accept(password, account);
    }

    public void insertBankLog(final String account, final String title, final String details) {
        final BankAccount bankAccount = this.getAccount(account);
        final String logId = account.split("-")[0] + "-" + account.split("-")[1] + "-" + this.createKey(5);

        final List<BankLog> logs = bankAccount.getBankLogs();
        logs.add(new BankLog(logId, title, details, this.getDate()));
        bankAccount.setBankLogs(logs);

        CompletableFuture.runAsync(() -> {
            final List<String> dList = this.bankCollection.find(new Document("_id", account)).first().getList("logs", String.class);
            dList.add(logId + ";-;" + title + ";-;" + details + ";-;" + this.getDate());

            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("logs", dList)));
        });
    }

    public BankAccount getAccount(final String account) {
        return this.bankAccounts.get(account);
    }

    public void withdrawMoney(final String account, final double amount) {
        final BankAccount bankAccount = this.bankAccounts.get(account);
        bankAccount.setBalance(bankAccount.getBalance() - amount);

        CompletableFuture.runAsync(() -> {
            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("balance", bankAccount.getBalance())));
        });
    }

    public void depositMoney(final String account, final double amount) {
        final BankAccount bankAccount = this.bankAccounts.get(account);
        bankAccount.setBalance(bankAccount.getBalance() + amount);

        CompletableFuture.runAsync(() -> {
            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("balance", bankAccount.getBalance())));
        });
    }

    public void setMoney(final String account, final double amount) {
        final BankAccount bankAccount = this.bankAccounts.get(account);
        bankAccount.setBalance(amount);

        CompletableFuture.runAsync(() -> {
            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("balance", bankAccount.getBalance())));
        });
    }

    public void changePassword(final String account, final String password) {
        final BankAccount bankAccount = this.bankAccounts.get(account);
        bankAccount.setPassword(password);

        CompletableFuture.runAsync(() -> {
            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("password", bankAccount.getPassword())));
        });
    }

    public void changeDisplayName(final String account, final String displayName) {
        final BankAccount bankAccount = this.bankAccounts.get(account);
        bankAccount.setDisplayName(displayName);

        CompletableFuture.runAsync(() -> {
            this.bankCollection.updateOne(new Document("_id", account), new Document("$set", new Document("displayName", bankAccount.getDisplayName())));
        });
    }

    private String createKey(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345678901234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    private String createNumberKey(final int i) {
        final String chars = "1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String getDate() {
        final Date now = new Date();
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        return dateFormat.format(now);
    }

}
