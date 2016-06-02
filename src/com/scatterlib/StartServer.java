package com.scatterlib;

import com.scatterlib.net.ScatterServer;

public class StartServer {


    private static ScatterServer server;

    public static void main(String[] args) {
        Integer[] data = {0, 1, 1, 1, 1, 2, 2, 2, 2, 3};
        server = new ScatterServer(ScatterServer.SUM, data, 5, Integer.class);
        server.addClass(Work.class);
        server.start();
    }
}
