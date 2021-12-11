package net.eltown.apiserver.components.handler.association.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
@Data
public class Association {

    private final String id;
    private String owner;
    private List<Member> members;
    private List<String> pinboard;
    private int level;
    private double experience;
    private HashMap<Association.Settings.Entry, Settings> settings;

    @AllArgsConstructor
    @Data
    public static class Member {

        private final String member;
        private Role role;

        public enum Role {

            MEMBER,
            STAFF,
            EXECUTIVE_BOARD

        }

    }

    @AllArgsConstructor
    @Data
    public static class Settings<T> {

        private Object value;
        private final Class<T> clazz;

        public enum Entry {

            DISPLAY_NAME(String.class, "null"),
            DISPLAY_DESCRIPTION(String.class, "null"),
            DISPLAY_TAG(String.class, "null"),
            BANK_ACCOUNT(String.class, "null"),
            BANK_ACCOUNT_LIMIT(Double.class, 750.0),
            JOINING_FEE(Double.class, 0d),
            LEAVING_FEE(Double.class, 0d),
            CONDITION_PLAY_TIME(Integer.class, 1),
            CONDITION_LEVEL(Integer.class, 1),
            MINIMUM_LEVEL_CHARGE(Integer.class, 15),
            UPGRADE_XP_BOOST(Double.class, 0d),
            MAXIMUM_MEMBERS(Integer.class, 3),
            PINBOARD_LIMIT(Integer.class, 3),
            PERMISSION_KICK(String.class, "STAFF"),
            PERMISSION_INVITE(String.class, "EXECUTIVE_BOARD");

            private final Class defaultClass;
            private final Object defaultEntry;


            <T> Entry(Class<T> clazz, final Object defaultEntry) {
                this.defaultClass = clazz;
                this.defaultEntry = defaultEntry;
            }

            public Object defaultEntry() {
                return defaultEntry;
            }

            public Class defaultClass() {
                return defaultClass;
            }
        }

    }

}
