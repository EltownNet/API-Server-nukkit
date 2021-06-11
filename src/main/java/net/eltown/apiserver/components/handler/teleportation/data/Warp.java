package net.eltown.apiserver.components.handler.teleportation.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Warp {

    private final String name;
    private final String server;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double pitch;

}
