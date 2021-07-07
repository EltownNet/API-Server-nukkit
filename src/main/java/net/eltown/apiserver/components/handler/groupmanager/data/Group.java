package net.eltown.apiserver.components.handler.groupmanager.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Group {

    private final String name;
    private String prefix;
    private List<String> permissions;
    private List<String> inheritances;

}
