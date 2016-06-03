package com.scatterlib;

import com.scatterlib.net.ScatterClient;

public class StartClient {

    private static ScatterClient client;

    public static void main(String[] args) {
        client = new ScatterClient("127.0.0.1", 4);
        client.start(2);
    }
}
