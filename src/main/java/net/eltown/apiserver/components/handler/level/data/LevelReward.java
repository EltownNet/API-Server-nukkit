package net.eltown.apiserver.components.handler.level.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class LevelReward {

    private final int id;
    private String description;
    private String data;

}
