package net.eltown.apiserver.components.tinyrabbit.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Delivery {

    private final String key;
    private final String[] data;

}
