package net.eltown.apiserver.components.handler.giftkeys.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Giftkey {

    private final String key;
    private int maxUses;
    private List<String> uses;
    private List<String> rewards;
    private List<String> marks;

}
