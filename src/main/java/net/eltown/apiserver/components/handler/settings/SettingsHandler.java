package net.eltown.apiserver.components.handler.settings;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class SettingsHandler {

    private final Server server;
    private final SettingsProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public SettingsHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new SettingsProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (SettingsCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_UPDATE_SETTINGS:
                        this.provider.updateEntry(d[1], d[2], d[3]);
                        break;
                    case REQUEST_REMOVE_SETTINGS:
                        this.provider.removeEntry(d[1], d[2]);
                        break;
                    case REQUEST_UPDATE_ALL:
                        this.provider.updateAll(d[1], d[2]);
                        break;
                }
            }, "API/Settings[Receive]", "api.settings.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                final String[] d = request.getData();
                switch (SettingsCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_SETTINGS:
                        if (!this.provider.cachedSettings.containsKey(d[1])) this.provider.createAccountSettings(d[1]);
                        final StringBuilder builder = new StringBuilder();
                        this.provider.cachedSettings.get(d[1]).getSettings().forEach((key, value) -> {
                            builder.append(key).append(":").append(value).append(">:<");
                        });
                        if (builder.toString().isEmpty()) builder.append("null>:<");
                        final String settings = builder.substring(0, builder.length() - 3);
                        request.answer(SettingsCalls.CALLBACK_SETTINGS.name(), settings);
                        break;
                    case REQUEST_ENTRY:
                        request.answer(SettingsCalls.CALLBACK_ENTRY.name(), this.provider.getEntry(d[1], d[2], d[3]));
                        break;
                }
            }, "API/Settings[Callback]", "api.settings.callback");
        });
    }
}
