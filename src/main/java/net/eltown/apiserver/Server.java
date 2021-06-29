package net.eltown.apiserver;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.handler.crypto.CryptoHandler;
import net.eltown.apiserver.components.handler.economy.EconomyHandler;
import net.eltown.apiserver.components.handler.groupmanager.GroupHandler;
import net.eltown.apiserver.components.handler.player.PlayerHandler;
import net.eltown.apiserver.components.handler.teleportation.TeleportationHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    @Getter
    private ExecutorService executor;

    private Connection connection;
    @Getter
    private Config config;

    private EconomyHandler economyHandler;
    private PlayerHandler playerHandler;
    private GroupHandler groupHandler;
    private TeleportationHandler teleportationHandler;
    private CryptoHandler cryptoHandler;

    @SneakyThrows
    public void start() {
        this.log("Server wird gestartet...");
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.log(Runtime.getRuntime().availableProcessors() + " Threads erstellt.");
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        config.reload();
        config.save();
        if (!config.exists("MongoDB")) {
            config.set("MongoDB.Uri", "mongodb://root:Qco7TDqoYq3RXq4pA3y7ETQTK6AgqzmTtRGLsgbN@45.138.50.23:27017/admin?authSource=admin");
            config.set("MongoDB.PlayerDB", "eltown");
            config.set("MongoDB.CryptoDB", "crypto");
            config.set("MongoDB.EconomyDB", "eltown");
            config.set("MongoDB.GroupDB", "eltown");
            config.set("MongoDB.CryptoDB", "eltown");
        }
        config.save();

        this.log("Verbinde zu RabbitMQ...");
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        this.connection = factory.newConnection("API Server");
        this.log("Erfolgreich mit RabbitMQ verbunden.");

        this.log("Starte EconomyHandler...");
        this.economyHandler = new EconomyHandler(this, this.connection);
        this.log("EcomomyHandler erfolgreich gestartet.");

        this.log("Starte CryptoHandler...");
        this.cryptoHandler = new CryptoHandler(this);
        this.log("CryptoHandler erfolgreich gestartet.");

        this.log("Starte PlayerHandler...");
        this.playerHandler = new PlayerHandler(this, this.connection);
        this.log("PlayerHandler erfolgreich gestartet.");

        this.log("Starte GroupHandler...");
        this.groupHandler = new GroupHandler(this);
        this.log("GroupHandler erfolgreich gestartet.");

        this.log("Starte TeleportationHandler...");
        this.teleportationHandler = new TeleportationHandler(this);
        this.log("TeleportationHandler erfolgreich gestartet.");

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

    public void log(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                LocalDateTime time = timestamp.toLocalDateTime();
                System.out.println(Colors.ANSI_CYAN + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + Colors.ANSI_WHITE + " [" + Colors.ANSI_BLUE + "LOG" + Colors.ANSI_WHITE + "] " + message);
                String file = time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " [LOG] " + message + "\n";
                Files.write(Paths.get("server.log"),
                        file.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

}
