package net.eltown.apiserver.components.handler.crypto;

import com.rabbitmq.client.Connection;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.crypto.data.Transaction;
import net.eltown.apiserver.components.handler.crypto.data.TransferPrices;
import net.eltown.apiserver.components.handler.crypto.data.Wallet;
import net.eltown.apiserver.components.handler.crypto.data.Worth;
import net.eltown.apiserver.components.handler.economy.EconomyProvider;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

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
                try {

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
                        case REQUEST_WORTH:
                            final Worth w = this.provider.getWorth();
                            request.answer(CryptoCalls.REQUEST_WORTH.name(), "" + w.getCtc(), "" + w.getElt(), "" + w.getNot());
                            break;
                        case REQUEST_TRANSFER_PRICES:
                            final TransferPrices t = this.provider.getTransferPrices();
                            request.answer(CryptoCalls.REQUEST_TRANSFER_PRICES.name(), "" + t.getSlow(), "" + t.getNormal(), "" + t.getFast());
                            break;
                        case REQUEST_TRANSACTIONS:
                            final StringBuilder sb = new StringBuilder();

                            this.provider.getTransactions(data[1]).forEach((e) -> {
                                sb.append(e.getId())
                                        .append(">>")
                                        .append(e.getAmount())
                                        .append(">>")
                                        .append(e.getWorth())
                                        .append(">>")
                                        .append(e.getType())
                                        .append(">>")
                                        .append(e.getFrom())
                                        .append(">>")
                                        .append(e.getTo())
                                        .append(">>")
                                        .append(e.getMinutesLeft())
                                        .append(">>")
                                        .append(e.isCompleted())
                                        .append("&");
                            });

                            request.answer(CryptoCalls.REQUEST_TRANSACTIONS.name(), sb.length() > 3  ? sb.substring(0, sb.length() - 1) : "null");
                            break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "API/Crypto", "crypto.callback");
        });

        /*
        * ```yaml
_id: "asjfkjnbn34234n45"
amount: 3
worth: 39.99
type: "CTC/ELT/NOT"
to: "JanIstSüß"
from: "Justin"
minutesLeft: 30
completed: true
```*/

        this.server.getExecutor().execute(() -> {
            this.listener.receive((delivery) -> {
                try {
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
                        case UPDATE_TRANSFER_CRYPTO:
                            final String id = this.createID(10);
                            final String asset = data[1];
                            final double amount = Double.parseDouble(data[2]);
                            final double worth = Double.parseDouble(data[3]);
                            final String from = data[4];
                            final String to = data[5];
                            final int time = Integer.parseInt(data[6]);

                            this.provider.addTransaction(new Transaction(id, amount, worth, asset, from, to, time, false));
                            break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }, "API/Crypto", "crypto.receive");
        });
    }

    private String createID(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return this.getMD5Hash(stringBuilder.toString());
    }

    private String getMD5Hash(String data) {
        String result = null;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * Use javax.xml.bind.DatatypeConverter class in JDK to convert byte array
     * to a hexadecimal string. Note that this generates hexadecimal in upper case.
     *
     * @param hash
     * @return
     */
    private String bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }


}
