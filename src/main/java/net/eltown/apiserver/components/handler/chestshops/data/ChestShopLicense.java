package net.eltown.apiserver.components.handler.chestshops.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChestShopLicense {

    private final String owner;
    private String license;
    private int additionalShops;

    public void addShops(final int i) {
        this.additionalShops += i;
    }

}
