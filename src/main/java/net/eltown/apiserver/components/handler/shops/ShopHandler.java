package net.eltown.apiserver.components.handler.shops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.CryptoProvider;
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
                    request.answer(ShopCalls.REQUEST_ITEM_PRICE.name(), "" +
                            this.provider.getPrice(new int[]{Integer.parseInt(data[1]), Integer.parseInt(data[2])})
                    );
                    break;
            }
        }, "API/Shops", "shops.callback");
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
            }

        }, "API/Shops", "shops.receive");
    }

}
