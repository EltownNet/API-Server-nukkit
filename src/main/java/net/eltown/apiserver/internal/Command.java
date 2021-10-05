package net.eltown.apiserver.internal;

import lombok.Getter;
import net.eltown.apiserver.Server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class Command {

    private final String name;
    private final Set<String> aliases;
    private final Server server = Server.getInstance();

    public Command(final String name, final String... aliases) {
        this.name = name;
        this.aliases = new HashSet<>(Arrays.asList(aliases));
    }

    public void execute(final String[] args) {

    }

}
