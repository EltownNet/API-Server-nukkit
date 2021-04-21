package net.eltown.apiserver.components.handler.groupmanager.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class GroupedPlayer {

    private final String player;
    private String group;
    private long duration;

}
