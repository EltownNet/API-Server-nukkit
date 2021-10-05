package net.eltown.apiserver.components.data;

public enum LogLevel {

    DEBUG(4),
    LOW(3),
    MEDIUM(2),
    HIGH(1);

    public final int level;

    LogLevel(final int level) {
        this.level = level;
    }

}
