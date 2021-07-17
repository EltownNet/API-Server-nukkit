package net.eltown.apiserver.components.handler.player.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class SyncPlayer {

    private String invString, ecString, health, food, exp, level, effects, gamemode;
    //private float health;
    //private int food, exp, level;
    private boolean canSync;

}
