package net.eltown.apiserver.components.handler.advancements.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Advancement {

    private final String advancement;
    private final String type;
    private final String data;
    private String displayName;
    private String displayDescription;
    private int requiredProgress;

}
