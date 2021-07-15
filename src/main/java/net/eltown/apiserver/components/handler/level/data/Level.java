package net.eltown.apiserver.components.handler.level.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Level {

    private final String player;
    private int level;
    private double experience;

}
