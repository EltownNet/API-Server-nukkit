package net.eltown.apiserver.components.handler.quests.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class QuestPlayer {

    private final String player;
    private List<QuestPlayerData> questPlayerData;

    @AllArgsConstructor
    @Getter
    @Setter
    public static class QuestPlayerData {

        private String questNameId;
        private long expire;
        private int required;
        private int current;

    }

}
