package net.eltown.apiserver.components.handler.groupmanager;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.groupmanager.data.Group;
import net.eltown.apiserver.components.handler.groupmanager.data.GroupedPlayer;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.ArrayList;
import java.util.List;

public class GroupHandler {

    private final Server server;
    private final GroupProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public GroupHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.tinyRabbitListener.throwExceptions(true);
        this.provider = new GroupProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                switch (GroupCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_CHECK_FOR_CREATING:
                        if (!this.provider.playerExists(request.getData()[1])) {
                            this.provider.createPlayer(request.getData()[1]);
                        }
                        request.answer(GroupCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_SET_GROUP:
                        if (this.provider.groups.containsKey(request.getData()[2])) {
                            if (this.provider.groupedPlayers.containsKey(request.getData()[1])) {
                                if (!this.provider.isInGroup(request.getData()[1], request.getData()[2])) {
                                    this.provider.setGroup(request.getData()[1], request.getData()[2], Long.parseLong(request.getData()[4]));
                                    request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                                    this.provider.getTinyRabbit().send("groups.extern", GroupCalls.REQUEST_CHANGE_PLAYER_PREFIX.name(), request.getData()[1], request.getData()[2]);
                                } else request.answer(GroupCalls.CALLBACK_PLAYER_ALREADY_IN_GROUP.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_PLAYER_DOES_NOT_EXIST.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_ADD_PERMISSION:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            if (!this.provider.groups.get(request.getData()[1]).getPermissions().contains(request.getData()[2])) {
                                this.provider.addPermission(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_GROUP_PERMISSION_ALREADY_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_ADD_PLAYER_PERMISSION:
                        if (this.provider.groupedPlayers.containsKey(request.getData()[1])) {
                            if (!this.provider.groupedPlayers.get(request.getData()[1]).getPermissions().contains(request.getData()[2])) {
                                this.provider.addPlayerPermission(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_PLAYER_PERMISSION_ALREADY_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_PLAYER_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_ADD_INHERITANCE:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            if (!this.provider.groups.get(request.getData()[1]).getInheritances().contains(request.getData()[2])) {
                                this.provider.addInheritance(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_GROUP_INHERITANCE_ALREADY_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_REMOVE_PERMISSION:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            if (this.provider.groups.get(request.getData()[1]).getPermissions().contains(request.getData()[2])) {
                                this.provider.removePermission(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_GROUP_PERMISSION_NOT_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_REMOVE_PLAYER_PERMISSION:
                        if (this.provider.groupedPlayers.containsKey(request.getData()[1])) {
                            if (this.provider.groupedPlayers.get(request.getData()[1]).getPermissions().contains(request.getData()[2])) {
                                this.provider.removePlayerPermission(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_PLAYER_PERMISSION_NOT_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_PLAYER_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_REMOVE_INHERITANCE:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            if (this.provider.groups.get(request.getData()[1]).getInheritances().contains(request.getData()[2])) {
                                this.provider.removeInheritance(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_GROUP_INHERITANCE_NOT_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_FULL_GROUP:
                        final Group group = this.provider.groups.get(request.getData()[1]);
                        final List<String> lPermissions = new ArrayList<>(group.getPermissions());

                        for (final String s : group.getInheritances()) {
                            final Group iGroup = this.provider.groups.get(s);
                            for (final String f : iGroup.getPermissions()) {
                                if (!lPermissions.contains(f)) lPermissions.add(f);
                            }
                        }

                        final StringBuilder fPermissions = new StringBuilder();
                        for (final String s : lPermissions) {
                            fPermissions.append(s).append("#");
                        }

                        final StringBuilder fInheritances = new StringBuilder();
                        for (final String s : group.getInheritances()) {
                            fInheritances.append(s).append("#");
                        }

                        request.answer(GroupCalls.CALLBACK_FULL_GROUP.name(), group.getName(), group.getPrefix(), fPermissions.toString(), fInheritances.toString());
                        break;
                    case REQUEST_FULL_GROUP_PLAYER:
                        try {
                            if (!this.provider.playerExists(request.getData()[1])) {
                                this.provider.createPlayer(request.getData()[1]);
                            }

                            final GroupedPlayer player = this.provider.groupedPlayers.get(request.getData()[1]);
                            final Group group2 = this.provider.groups.get(player.getGroup());
                            final List<String> lPermissions2 = new ArrayList<>(group2.getPermissions());

                            for (final String s : group2.getInheritances()) {
                                final Group iGroup = this.provider.groups.get(s);
                                for (final String f : iGroup.getPermissions()) {
                                    if (!lPermissions2.contains(f)) lPermissions2.add(f);
                                }
                            }

                            final StringBuilder fPermissions2 = new StringBuilder();
                            for (final String s : lPermissions2) {
                                fPermissions2.append(s).append("#");
                            }

                            final StringBuilder fInheritances2 = new StringBuilder();
                            for (final String s : group2.getInheritances()) {
                                fInheritances2.append(s).append("#");
                            }

                            final StringBuilder fAdditionalPermissions2 = new StringBuilder();
                            for (final String s : player.getPermissions()) {
                                fAdditionalPermissions2.append(s).append("#");
                            }

                            request.answer(GroupCalls.CALLBACK_FULL_GROUP_PLAYER.name(), group2.getName(), String.valueOf(player.getDuration()), group2.getPrefix(), fPermissions2.toString(), fInheritances2.toString(), fAdditionalPermissions2.toString());
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_GROUP_EXISTS:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            request.answer(GroupCalls.CALLBACK_NULL.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_CREATE_GROUP:
                        if (!this.provider.groups.containsKey(request.getData()[1])) {
                            this.provider.createGroup(request.getData()[1], request.getData()[2]);
                            request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_ALREADY_EXIST.name(), "null");
                        break;
                    case REQUEST_REMOVE_GROUP:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            this.provider.removeGroup(request.getData()[1]);
                            request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_GROUPS:
                        final StringBuilder builder = new StringBuilder();

                        for (final String s : this.provider.groups.keySet()) {
                            builder.append(s).append("#");
                        }

                        request.answer(GroupCalls.CALLBACK_GROUPS.name(), builder.toString());
                        break;
                    case REQUEST_GET_PREFIX:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            request.answer(GroupCalls.CALLBACK_GET_PREFIX.name(), this.provider.groups.get(request.getData()[1]).getPrefix());
                        } else request.answer(GroupCalls.CALLBACK_NULL.name(), "null");
                        break;
                    case REQUEST_CHANGE_PREFIX:
                        this.provider.changePrefix(request.getData()[1], request.getData()[2]);
                        request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        break;
                }
            }), "API/GroupManager", "groups");
        });
    }

}
