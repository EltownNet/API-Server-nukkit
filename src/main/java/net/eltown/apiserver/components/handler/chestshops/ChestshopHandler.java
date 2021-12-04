package net.eltown.apiserver.components.handler.chestshops;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

public class ChestshopHandler {

    private final Server server;
    private final ChestshopProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public ChestshopHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new ChestshopProvider(server);
        this.startCallbacking();
    }

    private void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                final String[] d = delivery.getData();
                switch (ChestshopCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_CREATE_CHESTSHOP:
                        this.provider.createChestShop(Long.parseLong(d[1]), Double.parseDouble(d[2]), Double.parseDouble(d[3]),
                                Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]),
                                d[8], d[9], d[10], Integer.parseInt(d[11]), d[12], Double.parseDouble(d[13]), d[14]);
                        break;
                    case REQUEST_UPDATE_AMOUNT:
                        this.provider.updateCount(Long.parseLong(d[1]), Integer.parseInt(d[2]));
                        break;
                    case REQUEST_UPDATE_PRICE:
                        this.provider.updatePrice(Long.parseLong(d[1]), Double.parseDouble(d[2]));
                        break;
                    case REQUEST_UPDATE_ITEM:
                        this.provider.updateItem(Long.parseLong(d[1]), d[2]);
                        break;
                    case REQUEST_REMOVE_SHOP:
                        this.provider.removeChestShop(Long.parseLong(d[1]));
                        break;
                    case REQUEST_SET_LICENSE:
                        this.provider.setLicense(d[1], d[2]);
                        break;
                    case REQUEST_ADD_ADDITIONAL_SHOPS:
                        this.provider.addAdditionalShops(d[1], Integer.parseInt(d[2]));
                        break;
                    case REQUEST_SET_ADDITIONAL_SHOPS:
                        this.provider.setAdditionalShops(d[1], Integer.parseInt(d[2]));
                        break;
                }
            }, "API/ChestShop[Receive]", "api.chestshops.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback(request -> {
                final String[] d = request.getData();
                switch (ChestshopCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_LOAD_DATA:
                        final StringBuilder chestShopDataBuilder = new StringBuilder();
                        this.provider.cachedChestShops.forEach((id, chestShop) -> {
                            chestShopDataBuilder
                                    .append(id).append("#")
                                    .append(chestShop.getSignX()).append("#")
                                    .append(chestShop.getSignY()).append("#")
                                    .append(chestShop.getSignZ()).append("#")
                                    .append(chestShop.getChestX()).append("#")
                                    .append(chestShop.getChestY()).append("#")
                                    .append(chestShop.getChestZ()).append("#")
                                    .append(chestShop.getLevel()).append("#")
                                    .append(chestShop.getOwner()).append("#")
                                    .append(chestShop.getShopType()).append("#")
                                    .append(chestShop.getShopCount()).append("#")
                                    .append(chestShop.getItem()).append("#")
                                    .append(chestShop.getPrice()).append("#")
                                    .append(chestShop.getBankAccount()).append("-;-");
                        });
                        final String chestShopData = chestShopDataBuilder.substring(0, chestShopDataBuilder.length() - 3);

                        final StringBuilder chestShopLicenseBuilder = new StringBuilder();
                        this.provider.cachedChestShopLicenses.forEach((owner, license) -> {
                            chestShopLicenseBuilder
                                    .append(owner).append("#")
                                    .append(license.getLicense()).append("#")
                                    .append(license.getAdditionalShops()).append("-;-");
                        });
                        final String chestShopLicense = chestShopLicenseBuilder.substring(0, chestShopLicenseBuilder.length() - 3);

                        request.answer(ChestshopCalls.CALLBACK_LOAD_DATA.name(), chestShopData, chestShopLicense);
                        break;
                }
            }, "API/ChestShop[Callback]", "api.chestshops.callback");
        });
    }

}
