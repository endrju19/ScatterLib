package com.scatterlib.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.scatterlib.packets.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterServer {

    private final Server server;
    private Thread thread;
    private byte ID = 1;
    private byte taskID = 1;
    private int parametersID = 1;
    private ArrayList<Class> classes = new ArrayList<>();
    private Map<Byte, PacketInstruction> instructions = new HashMap<>();

    public ScatterServer() {
        Server tempServer = null;
        try {
            tempServer = new Server(99999999, 99999999);
        } catch (Exception exception) {
            cleanUp(exception);
        }
        this.server = tempServer;
        try {
            Log.set(Log.LEVEL_NONE);
            KryoUtil.registerServerClasses(server);
            Listener listener = new Listener() {

                @Override
                public synchronized void received(Connection connection, Object obj) {
                    if (obj instanceof PacketGetInstruction) {
                        connection.sendTCP(instructions.get((byte) 1));
                    } else if (obj instanceof PacketGetWork) {
                        PacketInstruction inst = instructions.get((byte) 1);
                        connection.sendTCP(new PacketWork(inst.getID(), 0, parametersID++, 1));
                        connection.sendTCP(new PacketData("ABC"));
                    } else if (obj instanceof PacketResult) {
                        combineResult((PacketResult) obj);
                    } else if (obj instanceof PacketJoinRequest) {
                        System.out.println("Join message: " + ((PacketJoinRequest) obj).getMessage());
                        connection.sendTCP(new PacketJoinResponse(ID++));
                    }
                }

                @Override
                public synchronized void connected(Connection connection) {
                    System.out.println("Received a connection from " + connection.getRemoteAddressTCP().getHostString() + " (" + connection.getID() + ")");
                }

                @Override
                public synchronized void disconnected(Connection connection) {
                    System.out.println("Disconnected: " + connection.toString());
                }

            };
            server.addListener(listener);
            try {
                server.bind(KryoUtil.TCP_PORT, KryoUtil.UDP_PORT);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                return;
            }
            System.out.println("Server started!");
        } catch (Exception ex) {
            cleanUp(ex);
        }
    }

    private void combineResult(PacketResult result) {
//        TODO
    }


    public void addClass(Class c) {
        classes.add(c);
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("src/" + c.getName().replace(".", "/") + ".java"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String text = scanner.useDelimiter("\\A").next();
        scanner.close();
        instructions.put(taskID, new PacketInstruction(taskID, c.getName(), text));
        taskID++;
    }

    public synchronized void start() {
        thread = new Thread(server, "Server");
        try {
            thread.start();
        } catch (Exception ex) {
            cleanUp(ex);
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
