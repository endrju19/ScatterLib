package com.scatterlib.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterClient {

    private final Client client;

    public ScatterClient(String IP) {
        Client temp = null;
        try {
            temp = new Client(99999999, 99999999);
        } catch (Exception ex) {
            cleanUp(ex.getMessage());
        }
        client = temp;

        try {
            Log.set(Log.LEVEL_DEBUG);
            KryoUtil.registerClientClass(client);
            client.start();
            client.addListener(new Listener() {
                @Override
                public void connected(Connection connection) {

                }

                @Override
                public void received(Connection connection, Object obj) {

                }

                @Override
                public void disconnected(Connection connection) {

                }
            });
            try {
                /* Make sure to connect using both tcp and udp port */
                client.connect(5000, IP, KryoUtil.TCP_PORT, KryoUtil.UDP_PORT);
            } catch (IOException exception) {
                cleanUp(exception.getMessage());
                return;
            }
            client.getUpdateThread().setUncaughtExceptionHandler((Thread thread, Throwable exception) -> {
                cleanUp(exception.getMessage());
            });
        } catch (Exception ex) {
            cleanUp(ex.getMessage());
        }
    }

    public synchronized void stop() {
        client.stop();
        client.close();
    }

    private synchronized void cleanUp(String msg) {
        stop();
        System.err.println(msg);
    }

}
