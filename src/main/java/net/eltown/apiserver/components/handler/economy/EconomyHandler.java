package net.eltown.apiserver.components.handler.economy;

import com.rabbitmq.client.*;
import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;


public class EconomyHandler {

    private final Server server;
    private final EconomyProvider provider;
    private final TinyRabbitListener listener;

    @SneakyThrows
    public EconomyHandler(final Server server, final Connection connection) {
        this.server = server;
        this.provider = new EconomyProvider(server);
        this.listener = new TinyRabbitListener("localhost");
        this.startCallbacking();
    }

    public void startCallbacking() {
        server.getExecutor().execute(() -> {
            this.listener.callback((request -> {
                switch (EconomyCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_GETMONEY:
                        request.answer(EconomyCalls.CALLBACK_MONEY.name(), String.valueOf(this.provider.get(request.getData()[1])));
                        break;
                    case REQUEST_SETMONEY:
                        this.provider.set(request.getData()[1], Double.parseDouble(request.getData()[2]));
                        request.answer(EconomyCalls.CALLBACK_NULL.name());
                        break;
                    case REQUEST_ACCOUNTEXISTS:
                        request.answer(EconomyCalls.CALLBACK_ACCOUNTEXISTS.name(), String.valueOf(this.provider.has(request.getData()[1])));
                        break;
                    case REQUEST_CREATEACCOUNT:
                        this.provider.create(request.getData()[1], Double.parseDouble(request.getData()[2]));
                        request.answer(EconomyCalls.CALLBACK_NULL.name());
                        break;
                    case REQUEST_GETALL:
                        request.answer(EconomyCalls.CALLBACK_GETALL.name(), this.provider.getAll().toArray(new String[0]));
                        break;
                }
            }), "API Server", "economy");
        });
    }

}
