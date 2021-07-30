package net.eltown.apiserver.components.handler.bank.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BankLog {

    private final String logId;
    private final String title;
    private final String details;
    private final String date;

}
