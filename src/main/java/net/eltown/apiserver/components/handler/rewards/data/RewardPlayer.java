package net.eltown.apiserver.components.handler.rewards.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RewardPlayer {

    private final String player;
    private int day;
    private long lastReward;
    private long onlineTime;

}
