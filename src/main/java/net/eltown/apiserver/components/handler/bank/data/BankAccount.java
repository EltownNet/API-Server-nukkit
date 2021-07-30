package net.eltown.apiserver.components.handler.bank.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BankAccount {

    private final String account;
    private final String owner;
    private String password;
    private double balance;
    private List<BankLog> bankLogs;

}
