package com.scatterlib.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import java.io.IOException;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterServer {

    private final Server server;
    private Thread thread;

    public ScatterServer() {
        Server tempServer = null;
        try {
            tempServer = new Server(99999999, 99999999);
        } catch (Exception exception) {
            cleanUp(exception);
        }
        this.server = tempServer;
        try {
            Log.set(Log.LEVEL_DEBUG);
            KryoUtil.registerServerClasses(server);
            server.addListener(new Listener() {
                @Override
                public synchronized void connected(Connection connection) {
                    System.out.println("Received a connection from " + connection.getRemoteAddressTCP().getHostString() + " (" + connection.getID() + ")");
                }

                @Override
                public synchronized void disconnected(Connection connection) {
                }

                @Override
                public synchronized void received(Connection connection, Object obj) {
                }

            });
            try {
                server.bind(KryoUtil.TCP_PORT, KryoUtil.UDP_PORT);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                return;
            }
            System.out.println("Server started!");
        } catch (Exception e) {
            cleanUp(e);
        }
    }

    public synchronized void start() {
        thread = new Thread(server, "Server");
        try {
            thread.start();
        } catch (Exception e) {
            cleanUp(e);
        }
    }

    public synchronized void stop() {
        server.stop();
        server.close();
        thread.stop();
        thread.destroy();
        thread = null;
    }

    private synchronized void cleanUp(Exception ex) {
        stop();
        System.err.println(ex.getMessage());
    }

}
