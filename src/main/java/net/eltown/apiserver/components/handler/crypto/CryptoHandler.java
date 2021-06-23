package net.eltown.apiserver.components.handler.crypto;

import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import net.eltown.apiserver.components.handler.economy.EconomyProvider;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class CryptoHandler {

    private final Server server;
    private final CryptoProvider provider;
    private final TinyRabbitListener listener;

    @SneakyThrows
    public CryptoHandler(final Server server) {
        this.server = server;
        this.provider = new CryptoProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.listener.callback((request) -> {

                final String[] data = request.getData();

                switch (CryptoCalls.valueOf(request.getKey())) {
                    case REQUEST_WALLET:
                        final Wallet wallet = this.provider.getWallet(data[1]);
                        request.answer(CryptoCalls.REQUEST_WALLET.name(), wallet.getOwner(), String.valueOf(wallet.getCtc()), String.valueOf(wallet.getElt()), String.valueOf(wallet.getNot()));
                        break;
                    case REQUEST_UPDATE_WALLET:
                        this.provider.updateWallet(new Wallet(
                                data[1],
                                Double.parseDouble(data[2]),
                                Double.parseDouble(data[3]),
                                Double.parseDouble(data[4])
                        ));
                        request.answer(CryptoCalls.REQUEST_UPDATE_WALLET.name(), "null");
                        break;
                }

            }, "API/Crypto", "crypto.callback");
        });

        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery) -> {

                final String[] data = delivery.getData();

                switch (CryptoCalls.valueOf(delivery.getKey())) {
                    case UPDATE_WALLET:
                        this.provider.updateWallet(new Wallet(
                                data[1],
                                Double.parseDouble(data[2]),
                                Double.parseDouble(data[3]),
                                Double.parseDouble(data[4])
                        ));
                        break;
                }
            }, "API/Crypto", "crypto.receive");
        });
    }

}
