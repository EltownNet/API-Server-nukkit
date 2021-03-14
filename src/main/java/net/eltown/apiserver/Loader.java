package net.eltown.apiserver;

import java.util.Scanner;

public class Loader {

    private static Scanner scanner;
    private static Server server;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        server = new Server();
        server.start();
        read();
    }

    private static void read() {
        String input = scanner.nextLine();
        if (input.equalsIgnoreCase("stop")) {
            server.stop();
            return;
        }
        read();
    }

}
