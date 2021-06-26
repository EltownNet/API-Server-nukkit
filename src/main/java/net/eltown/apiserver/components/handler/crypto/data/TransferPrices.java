package net.eltown.apiserver.components.handler.crypto.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class TransferPrices {

    private double slow, normal, fast;

}
