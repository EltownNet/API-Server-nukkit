package net.eltown.apiserver.components.handler.teleportation.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Warp {

    private final String name;
    private String displayName;
    private String imageUrl;
    private String server;
    private String world;
    private double x;
    private double y;
    private double z;
    private double yaw;
    private double pitch;

}
