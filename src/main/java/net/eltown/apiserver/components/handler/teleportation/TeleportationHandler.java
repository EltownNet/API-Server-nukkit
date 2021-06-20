package net.eltown.apiserver.components.handler.teleportation;

import lombok.SneakyThrows;
import net.eltown.apiserver.Server;
import net.eltown.apiserver.components.handler.teleportation.data.Home;
import net.eltown.apiserver.components.handler.teleportation.data.Warp;
import net.eltown.apiserver.components.tinyrabbit.TinyRabbitListener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

public class TeleportationHandler {

    private final Server server;
    private final TeleportationProvider provider;
    private final TinyRabbitListener tinyRabbitListener;

    @SneakyThrows
    public TeleportationHandler(final Server server) {
        this.server = server;
        this.tinyRabbitListener = new TinyRabbitListener("localhost");
        this.provider = new TeleportationProvider(server);
        this.startCallbacking();
    }

    public void startCallbacking() {
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.receive(delivery -> {
                switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case REQUEST_DELETE_HOME:
                        this.provider.deleteHome(delivery.getData()[1], delivery.getData()[2]);
                        break;
                    case REQUEST_RENAME_HOME:
                        this.provider.updateHomeName(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3]);
                        break;
                    case REQUEST_UPDATE_POSITION:
                        this.provider.updateHomePosition(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3], delivery.getData()[4], Double.parseDouble(delivery.getData()[5]),
                                Double.parseDouble(delivery.getData()[6]), Double.parseDouble(delivery.getData()[7]), Double.parseDouble(delivery.getData()[8]), Double.parseDouble(delivery.getData()[9]));
                        break;
                    case REQUEST_DELETE_ALL_SERVER_HOMES:
                        this.provider.deleteServerHomes(delivery.getData()[1]);
                        break;
                    case REQUEST_DELETE_WARP:
                        this.provider.deleteWarp(delivery.getData()[1]);
                        break;
                    case REQUEST_RENAME_WARP:
                        this.provider.updateWarpName(delivery.getData()[1], delivery.getData()[2]);
                        break;
                    case REQUEST_UPDATE_WARP_IMAGE:
                        this.provider.updateWarpImage(delivery.getData()[1], delivery.getData()[2]);
                        break;
                    case REQUEST_UPDATE_WARP_POSITION:
                        this.provider.updateWarpPosition(delivery.getData()[1], delivery.getData()[2], delivery.getData()[3], Double.parseDouble(delivery.getData()[4]),
                                Double.parseDouble(delivery.getData()[5]), Double.parseDouble(delivery.getData()[6]), Double.parseDouble(delivery.getData()[7]), Double.parseDouble(delivery.getData()[8]));
                        break;
                    case REQUEST_TELEPORT:
                        final String[] d = delivery.getData();
                        this.provider.cachedTeleportation.put(d[2], new Home(d[1], d[2], d[3], d[4], Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9])));

                        this.provider.tinyRabbit.send("proxy.teleportation", TeleportationCalls.REQUEST_TELEPORT.name(), d[2], d[3]);
                        break;
                }
            }, "API/Teleportation/Receive", "teleportation.receive");
        });
        this.server.getExecutor().execute(() -> {
            this.tinyRabbitListener.callback((request -> {
                this.server.log(request.getKey());
                switch (TeleportationCalls.valueOf(request.getKey().toUpperCase())) {
                    case REQUEST_ALL_HOMES:
                        final Set<Home> homes = this.provider.getHomes(request.getData()[1]);
                        if (homes.size() == 0) {
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else {
                            final LinkedList<String> list = new LinkedList<>();
                            for (final Home home : homes) {
                                list.add(home.getName() + ">>" + home.getPlayer() + ">>" + home.getServer() + ">>" + home.getWorld() + ">>" + home.getX() + ">>" + home.getY() + ">>" + home.getZ() + ">>" + home.getYaw() + ">>" + home.getPitch());
                            }
                            request.answer(TeleportationCalls.CALLBACK_ALL_HOMES.name(), list.toArray(new String[0]));
                        }
                        break;
                    case REQUEST_ADD_HOME:
                        if (!this.provider.homeExists(request.getData()[1], request.getData()[2])) {
                            this.provider.createHome(request.getData()[1], request.getData()[2], request.getData()[3], request.getData()[4], Double.parseDouble(request.getData()[5]),
                                    Double.parseDouble(request.getData()[6]), Double.parseDouble(request.getData()[7]), Double.parseDouble(request.getData()[8]), Double.parseDouble(request.getData()[9]));
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else request.answer(TeleportationCalls.CALLBACK_HOME_ALREADY_SET.name(), "null");
                        break;
                    case REQUEST_ALL_WARPS:
                        try {
                            this.server.log(request.getKey());
                            final Collection<Warp> warps = this.provider.warps.values();
                            if (warps.size() == 0) {
                                request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                            } else {
                                final LinkedList<String> list = new LinkedList<>();
                                for (final Warp warp : warps) {
                                    list.add(warp.getName() + ">>" + warp.getDisplayName() + ">>" + warp.getImageUrl() + ">>" + warp.getServer() + ">>" + warp.getWorld() + ">>" + warp.getX() + ">>" + warp.getY() + ">>" + warp.getZ() + ">>" + warp.getYaw() + ">>" + warp.getPitch());
                                }
                                request.answer(TeleportationCalls.CALLBACK_ALL_WARPS.name(), list.toArray(new String[0]));
                                this.server.log(String.join(",", list));
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REQUEST_ADD_WARP:
                        if (!this.provider.warpExists(request.getData()[1])) {
                            this.provider.createWarp(request.getData()[1], request.getData()[2], request.getData()[3], request.getData()[4], request.getData()[5], Double.parseDouble(request.getData()[6]),
                                    Double.parseDouble(request.getData()[7]), Double.parseDouble(request.getData()[8]), Double.parseDouble(request.getData()[9]), Double.parseDouble(request.getData()[10]));
                            request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        } else request.answer(TeleportationCalls.CALLBACK_WARP_ALREADY_SET.name(), "null");
                        break;
                    case REQUEST_CACHED_DATA:
                        if (this.provider.cachedTeleportation.containsKey(request.getData()[1])) {
                            final Home home = this.provider.cachedTeleportation.get(request.getData()[1]);
                            this.provider.cachedTeleportation.remove(request.getData()[1]);
                            request.answer(TeleportationCalls.CALLBACK_CACHED_DATA.name(), home.getName(), home.getWorld(), String.valueOf(home.getX()), String.valueOf(home.getY()), String.valueOf(home.getZ()),
                                    String.valueOf(home.getYaw()), String.valueOf(home.getPitch()));
                        } else request.answer(TeleportationCalls.CALLBACK_NULL.name(), "null");
                        break;
                }
            }), "API/Teleportation/Callback", "teleportation.callback");
        });
    }

}

