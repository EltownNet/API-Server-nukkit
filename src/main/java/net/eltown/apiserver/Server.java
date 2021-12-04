package net.eltown.apiserver;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.data.LogLevel;
import net.eltown.apiserver.components.handler.advancements.AdvancementsHandler;
import net.eltown.apiserver.components.handler.bank.BankHandler;
import net.eltown.apiserver.components.handler.chestshops.ChestshopHandler;
import net.eltown.apiserver.components.handler.crates.CratesHandler;
import net.eltown.apiserver.components.handler.crypto.CryptoHandler;
import net.eltown.apiserver.components.handler.drugs.DrugHandler;
import net.eltown.apiserver.components.handler.economy.EconomyHandler;
import net.eltown.apiserver.components.handler.friends.FriendHandler;
import net.eltown.apiserver.components.handler.giftkeys.GiftkeyHandler;
import net.eltown.apiserver.components.handler.groupmanager.GroupHandler;
import net.eltown.apiserver.components.handler.level.LevelHandler;
import net.eltown.apiserver.components.handler.player.PlayerHandler;
import net.eltown.apiserver.components.handler.quests.QuestHandler;
import net.eltown.apiserver.components.handler.rewards.RewardHandler;
import net.eltown.apiserver.components.handler.settings.SettingsHandler;
import net.eltown.apiserver.components.handler.shops.ShopHandler;
import net.eltown.apiserver.components.handler.teleportation.TeleportationHandler;
import net.eltown.apiserver.components.handler.ticketsystem.TicketHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Server {

    private static Server instance;

    private ExecutorService executor;

    private Connection connection;

    private Config config;

    private EconomyHandler economyHandler;
    private PlayerHandler playerHandler;
    private GroupHandler groupHandler;
    private TeleportationHandler teleportationHandler;
    private TicketHandler ticketHandler;
    private CryptoHandler cryptoHandler;
    private ShopHandler shopHandler;
    private GiftkeyHandler giftkeyHandler;
    private LevelHandler levelHandler;
    private DrugHandler drugHandler;
    private BankHandler bankHandler;
    private AdvancementsHandler advancementsHandler;
    private RewardHandler rewardHandler;
    private QuestHandler questHandler;
    private FriendHandler friendHandler;
    private SettingsHandler settingsHandler;
    private CratesHandler cratesHandler;
    private ChestshopHandler chestshopHandler;

    public Server() {
        instance = this;
    }

    @SneakyThrows
    public void start() {
        this.log("Server wird gestartet...");
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.log(4, Runtime.getRuntime().availableProcessors() + " Threads erstellt.");
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        config.reload();
        config.save();
        if (!config.exists("LogLevel")) config.set("LogLevel", 1);
        if (!config.exists("MongoDB")) {
            config.set("MongoDB.Uri", "mongodb://root:Qco7TDqoYq3RXq4pA3y7ETQTK6AgqzmTtRGLsgbN@45.138.50.23:27017/admin?authSource=admin");
            config.set("MongoDB.PlayerDB", "eltown");
            config.set("MongoDB.CryptoDB", "crypto");
            config.set("MongoDB.EconomyDB", "eltown");
            config.set("MongoDB.GroupDB", "eltown");
            config.set("MongoDB.CryptoDB", "eltown");
        }
        final int logLevel = config.getInt("LogLevel");
        Internal.LOG_LEVEL = logLevel == 1 ? LogLevel.HIGH : logLevel == 2 ? LogLevel.MEDIUM : logLevel == 3 ? LogLevel.LOW : LogLevel.DEBUG;
        config.save();

        this.log("Verbinde zu RabbitMQ...");
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        this.connection = factory.newConnection("API/System[Main]");
        this.log("Erfolgreich mit RabbitMQ verbunden.");

        this.log(4, "Starte EconomyHandler...");
        this.economyHandler = new EconomyHandler(this, this.connection);
        this.log(4, "EcomomyHandler erfolgreich gestartet.");

        this.log(4, "Starte CryptoHandler...");
        this.cryptoHandler = new CryptoHandler(this);
        this.log(4, "CryptoHandler erfolgreich gestartet.");

        this.log(4, "Starte PlayerHandler...");
        this.playerHandler = new PlayerHandler(this, this.connection);
        this.log(4, "PlayerHandler erfolgreich gestartet.");

        this.log(4, "Starte GroupHandler...");
        this.groupHandler = new GroupHandler(this);
        this.log(4, "GroupHandler erfolgreich gestartet.");

        this.log(4, "Starte TeleportationHandler...");
        this.teleportationHandler = new TeleportationHandler(this);
        this.log(4, "TeleportationHandler erfolgreich gestartet.");

        this.log(4, "Starte TicketHandler...");
        this.ticketHandler = new TicketHandler(this);
        this.log(4, "TicketHandler erfolgreich gestartet.");

        this.log(4, "Starte ShopHandler...");
        this.shopHandler = new ShopHandler(this);
        this.log(4, "ShopHandler erfolgreich gestartet.");

        this.log(4, "Starte GiftkeyHandler...");
        this.giftkeyHandler = new GiftkeyHandler(this);
        this.log(4, "GiftkeyHandler erfolgreich gestartet.");

        this.log(4, "Starte LevelHandler...");
        this.levelHandler = new LevelHandler(this);
        this.log(4, "LevelHandler erfolgreich gestartet.");

        this.log(4, "Starte DrugHandler...");
        this.drugHandler = new DrugHandler(this);
        this.log(4, "DrugHandler erfolgreich gestartet.");

        this.log(4, "Starte BankHandler...");
        this.bankHandler = new BankHandler(this);
        this.log(4, "BankHandler erfolgreich gestartet.");

        this.log(4, "Starte AdvancementHandler...");
        this.advancementsHandler = new AdvancementsHandler(this);
        this.log(4, "AdvancementHandler erfolgreich gestartet.");

        this.log(4, "Starte RewardHandler...");
        this.rewardHandler = new RewardHandler(this);
        this.log(4, "RewardHandler erfolgreich gestartet.");

        this.log(4, "Starte QuestHandler...");
        this.questHandler = new QuestHandler(this);
        this.log(4, "QuestHandler erfolgreich gestartet.");

        this.log(4, "Starte FriendHandler...");
        this.friendHandler = new FriendHandler(this);
        this.log(4, "FriendHandler erfolgreich gestartet.");

        this.log(4, "Starte SettingsHandler...");
        this.settingsHandler = new SettingsHandler(this);
        this.log(4, "SettingsHandler erfolgreich gestartet.");

        this.log(4, "Starte CratesHandler...");
        this.cratesHandler = new CratesHandler(this);
        this.log(4, "CratesHandler erfolgreich gestartet.");

        this.log(4, "Starte ChestShopHandler...");
        this.chestshopHandler = new ChestshopHandler(this);
        this.log(4, "ChestShopHandler erfolgreich gestartet.");

        this.log("Server wurde erfolgreich gestartet.");
        //this.log(this.getDataFolder());
    }

    @SneakyThrows
    public void stop() {
        this.log("Server wird gestoppt...");
        this.connection.close();
        this.log("Auf wiedersehen!");
    }

    @SneakyThrows
    public String getDataFolder() {
        return Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().replace("api-server.jar", "");
    }

    public static Server getInstance() {
        return instance;
    }

    public void log(final String message) {
        this.log(LogLevel.HIGH, message);
    }

    public void log(final int level, final String message) {
        this.log(level == 1 ? LogLevel.HIGH : level == 2 ? LogLevel.MEDIUM : level == 3 ? LogLevel.LOW : LogLevel.DEBUG, message);
    }

    public void log(final LogLevel logLevel, final String message) {
        CompletableFuture.runAsync(() -> {
            if (logLevel.level <= Internal.LOG_LEVEL.level) {
                try {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    LocalDateTime time = timestamp.toLocalDateTime();
                    System.out.println(Colors.ANSI_CYAN + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + Colors.ANSI_WHITE + " [" + Colors.ANSI_BLUE + "LOG" + Colors.ANSI_WHITE + "] " + message);
                    String file = time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " [LOG] " + message + "\n";
                    Files.write(Paths.get("logs/" + time.getDayOfMonth() + "-" + time.getMonth() + "-" + time.getYear() + ".log"),
                            file.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(Colors.ANSI_CYAN + "--------");
                    System.out.println(Colors.ANSI_CYAN + "HINWEIS: " + Colors.ANSI_RESET + " Falls es sich um den logs/XX-MONAT-XXXX Fehler handelt, erstelle den Ordner 'logs'.");
                    System.out.println(Colors.ANSI_CYAN + "--------");
                }
            }
        });
    }

}
