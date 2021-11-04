package net.eltown.apiserver.components.handler.shops.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ItemPrice {

    private int[] id;
    private double price, minBuy, minSell;
    private int bought, sold;

    public void addBought(final int amount) {
        this.bought += amount;
    }

    public void addSold(final int amount) {
        this.sold += amount;
    }

}
