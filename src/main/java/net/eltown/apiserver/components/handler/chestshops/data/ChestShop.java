package net.eltown.apiserver.components.handler.chestshops.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChestShop {

    private final long id;
    private double signX, signY, signZ;
    private double chestX, chestY, chestZ;
    private String level;
    private String owner;
    private String shopType;
    private int shopCount;
    private String item;
    private double price;
    private String bankAccount;

}
