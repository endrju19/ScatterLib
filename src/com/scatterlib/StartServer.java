package com.scatterlib;

import com.scatterlib.net.ScatterServer;

public class StartServer {


    private static ScatterServer server;

    public static void main(String[] args) {
        server = new ScatterServer();
        server.addClass(Work.class);
        server.start();
    }
}
