package net.eltown.apiserver.components.handler.groupmanager;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.groupmanager.data.Group;
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
                    case REQUEST_REMOVE_PERMISSION:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            if (this.provider.groups.get(request.getData()[1]).getPermissions().contains(request.getData()[2])) {
                                this.provider.removePermission(request.getData()[1], request.getData()[2]);
                                request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                            } else request.answer(GroupCalls.CALLBACK_GROUP_PERMISSION_NOT_ADDED.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                    case REQUEST_GROUP_PERMISSIONS:
                        final Group group = this.provider.groups.get(this.provider.groupedPlayers.get(request.getData()[1]).getGroup());
                        final List<String> list = new ArrayList<>(group.getPermissions());

                        for (final String s : group.getInheritances()) {
                            final Group iGroup = this.provider.groups.get(s);
                            for (final String f : iGroup.getPermissions()) {
                                if (!list.contains(f)) list.add(f);
                            }
                        }

                        request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        break;
                    case REQUEST_CREATE_GROUP:
                        if (!this.provider.groups.containsKey(request.getData()[1])) {
                            this.provider.createGroup(request.getData()[1]);
                            request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_ALREADY_EXIST.name(), "null");
                        break;
                    case REQUEST_REMOVE_GROUP:
                        if (this.provider.groups.containsKey(request.getData()[1])) {
                            this.provider.removeGroup(request.getData()[1]);
                            request.answer(GroupCalls.CALLBACK_SUCCESS.name(), "null");
                        } else request.answer(GroupCalls.CALLBACK_GROUP_DOES_NOT_EXIST.name(), "null");
                        break;
                }
            }), "API/GroupManager", "groups");
        });
    }

}
