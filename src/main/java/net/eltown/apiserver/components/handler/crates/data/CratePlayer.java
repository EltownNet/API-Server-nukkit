package net.eltown.apiserver.components.handler.crates.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
public class CratePlayer {

    private final String player;
    private Map<String, Integer> data;

}
