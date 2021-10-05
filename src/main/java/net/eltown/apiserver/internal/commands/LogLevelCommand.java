package net.eltown.apiserver.internal.commands;

import net.eltown.apiserver.Internal;
import net.eltown.apiserver.components.data.Colors;
import net.eltown.apiserver.components.data.LogLevel;
import net.eltown.apiserver.internal.Command;

public class LogLevelCommand extends Command {

    public LogLevelCommand() {
        super("loglevel", "ll");
    }

    @Override
    public void execute(String[] args) {
        try {
            final int level = Integer.parseInt(args[0]);
            if (!(level > 4 || level <= 0)) {
                Internal.LOG_LEVEL = level == 1 ? LogLevel.HIGH : level == 2 ? LogLevel.MEDIUM : level == 3 ? LogLevel.LOW : LogLevel.DEBUG;
                this.getServer().log(Colors.ANSI_CYAN + "HINWEIS: " + Colors.ANSI_RESET + "Das Log Level wurde auf " + Internal.LOG_LEVEL.name() + " gesetzt. Beachte das diese Änderung nur temporär ist und nach einem Neustart des API Servers rückgängig gemacht wird.");
            } else this.getServer().log(Colors.ANSI_RED + "Fehler: Ungültige Eingabe");
        } catch (final Exception ex) {
            this.getServer().log(Colors.ANSI_RED + "Fehler: Ungültige Eingabe");
        }
    }
}
