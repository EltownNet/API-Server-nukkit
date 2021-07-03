package net.eltown.apiserver.components.handler.shops;

import net.eltown.apiserver.Server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopTask {

    private final Timer timer;
    private final ShopProvider provider;
    private final double increaseFactor = 0.05;
    private final int amountFactor = 64;
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
                        final AtomicInteger count = new AtomicInteger();
                        server.log("Aktualisiere Shop Preise...");

                        provider.getPrices().values().forEach((e) -> {
                            final int toDevide = e.getBought() - e.getSold();
                            if (toDevide != 0) {

                                final double toMultiply = (double) toDevide / (double) amountFactor;
                                final double add = 1 + (toMultiply * increaseFactor);

                                e.setPrice(e.getPrice() * add);
                                provider.updatePrice(e);
                                count.incrementAndGet();
                            }
                        });

                        server.log(count.get() + " Shop Preise wurde aktualisiert.");
                    }
                },
                TimeUnit.HOURS.toHours(1), TimeUnit.HOURS.toHours(1));

        this.timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        final AtomicInteger count = new AtomicInteger();

                        server.log("Speichere veränderte Shop Preise...");

                        provider.getToUpdate().forEach((id) -> {
                            provider.updatePrice(provider.getPrices().get(id));
                            count.incrementAndGet();
                        });

                        server.log(count.get() + " Shop Preise wurden gespeichert.");
                    }
                },
                TimeUnit.MINUTES.toMinutes(5), TimeUnit.MINUTES.toMinutes(5));
    }

}
