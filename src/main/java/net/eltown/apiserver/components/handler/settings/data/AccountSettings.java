package net.eltown.apiserver.components.handler.settings.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class AccountSettings {

    private final String player;
    private Map<String, String> settings;

}
