package net.eltown.apiserver.components.handler.quests.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Quest {

    /*
    Quest-Data-Typen:

    - bring#<item>#<amount>
    - collect#<item>#<amount>
    - place#<item>#<amount>
    - explore#<pos1>#<pos2>
    - craft#<item>#<amount>
    - execute#<command>

    Quest-Reward-Data-Typen:
    - xp#<amount>
    - money#<amount>
    - item#<item>
    - gutschein#<gutscheinData>
    - permission#<key>#<description>

     * Inneres Trennzeichen: #
     * Äußeres Trennzeichen: -#-
     */

    private final String nameId;
    private String displayName;
    private String description;
    private String data;
    private int required;
    private long expire;
    private String rewardData;
    private String link;

}
