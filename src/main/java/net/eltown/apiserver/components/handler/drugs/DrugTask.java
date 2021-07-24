package net.eltown.apiserver.components.handler.drugs;

import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.CryptoProvider;
import net.eltown.apiserver.components.handler.crypto.data.Transaction;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import net.eltown.apiserver.components.handler.drugs.data.Delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DrugTask {

    private final Timer timer;
    private final DrugProvider provider;
    private final Server server;

    public DrugTask(final Server server, final DrugProvider provider) {
        this.server = server;
        this.provider = provider;
        this.timer = new Timer("Drugs Timer #1");
    }

    public void run() {
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {

                final List<Delivery> toUpdate = new ArrayList<>();
                final AtomicInteger count = new AtomicInteger();

                provider.getDeliveries().forEach((id, del) -> {
                    if (!del.isCompleted()) {
                        del.setTimeLeft(del.getTimeLeft() - 1);
                        toUpdate.add(del);

                        if (del.getTimeLeft() <= 0) {
                            del.setTimeLeft(0);
                            del.setCompleted(true);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    provider.updateDelivery(del);
                                }
                            }, 50L * count.incrementAndGet());
                        }
                    }
                });

                toUpdate.forEach((del) -> {
                    provider.getDeliveries().put(del.getId(), del);
                });
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));

        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {

                server.log("Speichere veränderte Lieferungen...");
                final AtomicInteger count = new AtomicInteger();

                provider.getDeliveries().values().forEach((delivery) -> {
                    if (!delivery.isCompleted()) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                provider.updateDelivery(delivery);
                            }
                        }, 50L * count.incrementAndGet());
                    }
                });
                server.log(count.get() + " Lieferungen gespeichert.");
            }
        }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
    }

}
