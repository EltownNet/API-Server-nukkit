package net.eltown.apiserver.components.handler.drugs.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class Delivery {

    private final String id, type, quality;

    private final int time;
    @Setter
    private int timeLeft;
    @Setter
    private boolean completed;

}
