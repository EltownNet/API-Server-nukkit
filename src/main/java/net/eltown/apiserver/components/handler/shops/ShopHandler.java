package net.eltown.apiserver.components.handler.shops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.shops.data.ItemPrice;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class ShopHandler {

    private final Server server;
    private final ShopProvider provider;
    private final TinyRabbitListener listener;

    @SneakyThrows
    public ShopHandler(final Server server) {
        this.server = server;
        this.provider = new ShopProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.listener.throwExceptions(true);
        this.startCallbacking();
        this.startListening();
    }

    public void startCallbacking() {
        this.listener.callback((request) -> {
            final String[] data = request.getData();
            switch (ShopCalls.valueOf(request.getKey())) {
                case REQUEST_ITEM_PRICE:
                    final double[] prices = this.provider.getPrice(new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])});

                    request.answer(ShopCalls.REQUEST_ITEM_PRICE.name(),
                            "" + prices[0],
                            "" + prices[1]
                    );
                    break;
                case REQUEST_MIN_BUY_SELL:
                    final ItemPrice price = this.provider.getPrices().get(data[1] + ":" + data[2]);

                    request.answer(ShopCalls.REQUEST_MIN_BUY_SELL.name(),
                            "" + price.getMinBuy(),
                            "" + price.getMinSell()
                    );
            }
        }, "API/Shops[Callback]", "api.shops.callback");
    }

    public void startListening() {
        this.listener.receive((delivery) -> {
            final String[] data = delivery.getData();
            switch (ShopCalls.valueOf(delivery.getKey())) {
                case UPDATE_ITEM_SOLD:
                    this.provider.addSold(
                            new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                            Integer.parseInt(data[3])
                    );
                    break;
                case UPDATE_ITEM_BOUGHT:
                    this.provider.addBought(
                            new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                            Integer.parseInt(data[3])
                    );
                    break;
                case UPDATE_ITEM_PRICE:
                    this.provider.setPrice(
                            new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                            Double.parseDouble(data[3])
                    );
                    break;
                case UPDATE_MIN_BUY:
                    this.provider.setMinBuy(
                            new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                            Double.parseDouble(data[3])
                    );
                    break;
                case UPDATE_MIN_SELL:
                    this.provider.setMinSell(
                            new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])},
                            Double.parseDouble(data[3])
                    );
                    break;
            }

        }, "API/Shops[Receive]", "api.shops.receive");
    }

}
