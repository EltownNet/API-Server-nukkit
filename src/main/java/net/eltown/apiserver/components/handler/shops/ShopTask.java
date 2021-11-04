package net.eltown.apiserver.components.handler.shops;

import net.eltown.apiserver.Server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopTask {

    private final Timer timer;
    private final ShopProvider provider;
    private final Server server;

    public ShopTask(final Server server, final ShopProvider provider) {
        this.server = server;
        this.provider = provider;
        this.timer = new Timer("Shop Timer #1");
    }

    public void run() {
        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        provider.updatePrices();
                    }
                },
                TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10));

        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        final AtomicInteger count = new AtomicInteger();

                        server.log(4, "Speichere veränderte Shop Preise...");

                        provider.getToUpdate().forEach((id) -> {
                            timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                provider.updatePrice(provider.getPrices().get(id));
                                count.incrementAndGet();
                            }
                        }, 50L * count.incrementAndGet());
                        });

                        provider.getToUpdate().clear();

                        server.log(4, count.get() + " Shop Preise wurden gespeichert.");
                    }
                },
                TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
    }

}
