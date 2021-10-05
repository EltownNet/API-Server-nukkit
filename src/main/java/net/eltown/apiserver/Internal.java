package net.eltown.apiserver;

import net.eltown.apiserver.components.data.LogLevel;
import net.eltown.apiserver.internal.CommandHandler;
import net.eltown.apiserver.internal.commands.LogLevelCommand;
import net.eltown.apiserver.internal.commands.TestCommand;

import java.util.Scanner;

public class Internal {

    public static LogLevel LOG_LEVEL;
    private CommandHandler commandHandler;
    private Scanner scanner;
    private Server server;


    public void init() {
        scanner = new Scanner(System.in);

        this.commandHandler = new CommandHandler();
        this.commandHandler.register(
                new TestCommand(), new LogLevelCommand()
        );

        this.server = Server.getInstance();

        this.read();
    }

    private void read() {
        String input = scanner.nextLine();
        if (input.equalsIgnoreCase("stop")) {
            Server.getInstance().stop();
            return;
        } else {
            this.commandHandler.handle(input);
        }
        read();
    }

}
