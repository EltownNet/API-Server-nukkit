package net.eltown.apiserver.components.handler.bank;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.bank.data.BankAccount;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class BankHandler {

    private final Server server;
    private final BankProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public BankHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new BankProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (BankCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_INSERT_LOG:
                        this.provider.insertBankLog(d[1], d[2], d[3]);
                        break;
                    case REQUEST_WITHDRAW_MONEY:
                        this.provider.withdrawMoney(d[1], Double.parseDouble(d[2]));
                        break;
                    case REQUEST_DEPOSIT_MONEY:
                        this.provider.depositMoney(d[1], Double.parseDouble(d[2]));
                        break;
                    case REQUEST_SET_MONEY:
                        this.provider.setMoney(d[1], Double.parseDouble(d[2]));
                        break;
                    case REQUEST_CHANGE_PASSWORD:
                        this.provider.changePassword(d[1], d[2]);
                        break;
                    case REQUEST_CHANGE_DISPLAY_NAME:
                        this.provider.changeDisplayName(d[1], d[2]);
                        break;
                }
            }, "API/Bank[Receive]", "api.bank.receive");
        });

        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                final String[] d = request.getData();
                switch (BankCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CREATE_ACCOUNT:
                        this.provider.createBankAccount(d[1], d[2], (password, account) -> {
                            request.answer(BankCalls.CALLBACK_CREATE_ACCOUNT.name(), password, account);
                        });
                        break;
                    case REQUEST_GET_BANK_ACCOUNT:
                        if (this.provider.bankAccounts.containsKey(d[1])) {
                            final BankAccount bankAccount = this.provider.getAccount(d[1]);
                            final StringBuilder logs = new StringBuilder();

                            bankAccount.getBankLogs().forEach(e -> {
                                logs.append(e.getLogId()).append(";").append(e.getTitle()).append(";").append(e.getDetails()).append(";").append(e.getDate()).append("--");
                            });
                            final String finalLogs = logs.substring(0, logs.length() - 3);

                            request.answer(BankCalls.CALLBACK_GET_BANK_ACCOUNT.name(), bankAccount.getAccount(), bankAccount.getDisplayName(), bankAccount.getOwner(), bankAccount.getPassword(), String.valueOf(bankAccount.getBalance()), finalLogs);
                        } else request.answer(BankCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_BANKACCOUNTS_BY_PLAYER:
                        request.answer(BankCalls.CALLBACK_BANKACCOUNTS_BY_PLAYER.name(), this.provider.getAccountsByPlayer(d[1]));
                        break;
                }
            }), "API/Bank[Callback]", "api.bank.callback");
        });
    }

}
