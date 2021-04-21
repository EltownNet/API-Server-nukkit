package net.eltown.apiserver.components.handler.groupmanager.data;

import lombok.Data;

import java.util.List;

@Data
public class Group {

    private final String name;
    private final List<String> permissions;
    private final List<String> inheritances;

}
