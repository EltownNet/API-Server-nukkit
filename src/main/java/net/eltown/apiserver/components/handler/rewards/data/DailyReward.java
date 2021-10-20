package net.eltown.apiserver.components.handler.rewards.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DailyReward {

    private final String description;
    private final String id;
    private final int day;
    private final int chance;
    private final String data;

}
