package net.eltown.apiserver;

import net.eltown.apiserver.components.data.Colors;

public class Loader {

    private static Server server;
    private static Internal internal;

    public static void main(String[] args) {
        server = new Server();
        server.start();

        internal = new Internal();
        internal.init();

        server.log(Colors.ANSI_CYAN + "Hallo!");
    }

}
