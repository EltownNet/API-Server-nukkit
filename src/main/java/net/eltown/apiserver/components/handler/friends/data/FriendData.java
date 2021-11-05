package net.eltown.apiserver.components.handler.friends.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class FriendData {

    private final String player;
    private List<String> friends;
    private List<String> requests;

}
