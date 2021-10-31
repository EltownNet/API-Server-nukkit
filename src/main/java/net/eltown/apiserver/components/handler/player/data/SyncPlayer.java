package net.eltown.apiserver.components.handler.player.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class SyncPlayer {

    private String invString, ecString, health, food, level, exp, effects, gamemode;
    private boolean canSync;

}
