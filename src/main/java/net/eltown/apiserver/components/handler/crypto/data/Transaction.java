package net.eltown.apiserver.components.handler.crypto.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Transaction {

    private final String id;
    private final double amount, worth;
    private final String type, from, to;
    @Setter
    private int minutesLeft;
    private final int minutes;
    @Setter
    private boolean completed;

}
