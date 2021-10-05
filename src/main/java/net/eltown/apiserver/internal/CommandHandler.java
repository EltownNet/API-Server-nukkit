package net.eltown.apiserver.internal;

import java.util.*;

public class CommandHandler {

    private Set<Command> commands = new HashSet<>();

    public void register(final Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public Command findCommand(final String name) {
        return this.commands.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name) || c.getAliases().contains(name.toLowerCase()))
                .findFirst().orElse(null);
    }

    public void handle(final String given) {
        final String[] split = given.split(" ");
        final ArrayList<String> preArgs = new ArrayList<>(Arrays.asList(split));
        preArgs.remove(0);
        final String[] args = new ArrayList<>(preArgs).toArray(new String[0]);

        final Command command = this.findCommand(split[0]);
        if (command != null) command.execute(args);

    }

}
