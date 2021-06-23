package net.eltown.apiserver.components.handler.teleportation.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Home {

    private String name;
    private final String player;
    private String server;
    private String world;
    private double x;
    private double y;
    private double z;
    private double yaw;
    private double pitch;

}
