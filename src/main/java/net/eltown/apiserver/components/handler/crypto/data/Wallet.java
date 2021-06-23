package net.eltown.apiserver.components.handler.crypto.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Wallet {

    private String owner;
    private double ctc, elt, not;

}
