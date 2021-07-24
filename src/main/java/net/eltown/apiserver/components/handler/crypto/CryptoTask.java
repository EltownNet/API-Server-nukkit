package net.eltown.apiserver.components.handler.crypto;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.data.Transaction;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CryptoTask {

    private final Timer timer;
    private final CryptoProvider provider;
    private final Server server;

    public CryptoTask(final Server server, final CryptoProvider provider) {
        this.server = server;
        this.provider = provider;
        this.timer = new Timer("Crypto Timer #1");
    }

    public void run() {
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {

                final List<Transaction> toUpdate = new ArrayList<>();
                final AtomicInteger count = new AtomicInteger();

                provider.getTransactions().forEach((key, trans) -> {
                    if (!trans.isCompleted()) {
                        trans.setMinutesLeft(trans.getMinutesLeft() - 1);
                        toUpdate.add(trans);

                        if (trans.getMinutesLeft() <= 0) {
                            trans.setMinutesLeft(0);
                            trans.setCompleted(true);

                            final Wallet wallet = provider.getWallet(trans.getTo());

                            switch (trans.getType()) {
                                case "CTC":
                                    wallet.setCtc(wallet.getCtc() + trans.getAmount());
                                    break;
                                case "ELT":
                                    wallet.setElt(wallet.getElt() + trans.getAmount());
                                    break;
                                case "NOT":
                                    wallet.setNot(wallet.getNot() + trans.getAmount());
                                    break;
                            }

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    provider.updateWallet(wallet);
                                    provider.updateTransaction(trans);
                                }
                            }, 50L * count.incrementAndGet());
                        }
                    }
                });

                toUpdate.forEach((tr) -> {
                    provider.getTransactions().put(tr.getId(), tr);
                });
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));

        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {

                server.log("Speichere veränderte Transaktionen...");
                final AtomicInteger count = new AtomicInteger();

                provider.getTransactions().values().forEach((transaction) -> {
                    if (!transaction.isCompleted()) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                provider.updateTransaction(transaction);
                            }
                        }, 50L * count.incrementAndGet());
                    }
                });
                server.log(count.get() + " Transaktionen gespeichert.");
            }
        }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
    }

}
