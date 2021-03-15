package net.eltown.apiserver;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.apiserver.components.config.Config;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.handler.economy.EconomyHandler;
import net.eltown.apiserver.components.handler.player.PlayerHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Server {

    private Connection connection;
    @Getter
    private Config config;

    @SneakyThrows
    public void start() {
        this.log("Server wird gestartet...");
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        config.reload();
        config.save();
        if (!config.exists("MongoDB")) {
            config.set("MongoDB.Uri", "ENTER");
            config.set("MongoDB.EconomyDB", "eltown");
        }
        config.save();

        this.log("Verbinde zu RabbitMQ...");
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("api");
        factory.setPassword(" ");
        this.connection = factory.newConnection("API Server");
        this.log("Erfolgreich mit RabbitMQ verbunden.");

        this.log("Starte EconomyHandler...");
        EconomyHandler economyHandler = new EconomyHandler(this, this.connection);
        this.log("EcomomyHandler erfolgreich gestartet.");

        this.log("Starte PlayerHandler...");
        PlayerHandler playerHandler = new PlayerHandler(this, this.connection);
        this.log("PlayerHandler erfolgreich gestartet.");

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

    @SneakyThrows
    public void log(String message) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        LocalDateTime time = timestamp.toLocalDateTime();
        System.out.println(Colors.ANSI_CYAN + time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + Colors.ANSI_WHITE + " [" + Colors.ANSI_BLUE + "LOG" + Colors.ANSI_WHITE + "] " + message);
        String file = time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " [LOG] " + message + "\n";
        Files.write(Paths.get("server.log"),
                file.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

}
