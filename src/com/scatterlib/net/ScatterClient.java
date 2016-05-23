package com.scatterlib.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.scatterlib.mdkt.compiler.InMemoryJavaCompiler;
import com.scatterlib.packets.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterClient {

    private final Client client;
    private Connection server;
    private Thread thread;
    private boolean running;
    private long lastTime = 0;
    private long delay = 1000;
    private boolean haveWork;
    private PacketWork work;
    private Map<Byte, Class> instructions = new HashMap<>();


    public ScatterClient(String IP) {
        Client temp = null;
        try {
            temp = new Client(99999999, 99999999);
        } catch (Exception ex) {
            cleanUp(ex.getMessage());
        }
        client = temp;
        try {
            Log.set(Log.LEVEL_NONE);
            KryoUtil.registerClientClass(client);
            client.start();
            Listener listener = new Listener() {
                @Override
                public void received(Connection connection, Object obj) {
                    if (obj instanceof PacketJoinResponse) {
                        server = connection;
                        System.out.println("My ID: " + ((PacketJoinResponse) obj).getID());
                    } else if (obj instanceof PacketWork) {
                        if (!haveWork) {
                            work = (PacketWork) obj;
                            haveWork = true;
                        } else {
                            connection.sendTCP(new PacketNoNeed());
                        }
                    } else if (obj instanceof PacketInstruction) {
                        addInstruction((PacketInstruction) obj);
                    }
                }

                @Override
                public void connected(Connection connection) {
                    PacketJoinRequest join = new PacketJoinRequest("Wanna work!");
                    client.sendTCP(join);
                    lastTime = System.currentTimeMillis();
                }

                @Override
                public void disconnected(Connection connection) {
                    System.out.println("Disconnected from server!");
                }
            };
            client.addListener(listener);
            try {
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

    private void addInstruction(PacketInstruction instruction) {
        if (instructions.get(instruction.getID()) == null) {
            Class<?> instructionClass = null;
            try {
                instructionClass = InMemoryJavaCompiler.compile(instruction.getName(), instruction.getWork());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (instructionClass != null) {
                instructions.put(instruction.getID(), instructionClass);
            }
        }
    }

    public synchronized void start() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                if (haveWork) {
                    work();
                } else {
                    if (System.currentTimeMillis() - lastTime > delay) {
                        if (server != null && server.isConnected()) {
                            lastTime = System.currentTimeMillis();
                            getWork();
                        }
                    }
                }
            }
        });
        thread.start();
    }

    private synchronized void getWork() {
        if (server != null && server.isConnected()) {
            PacketGetWork getWork = new PacketGetWork();
            server.sendTCP(getWork);
        }
    }

    public synchronized void work() {
        if (work != null) {
            Class<?> instruction;
            if ((instruction = instructions.get(work.getInstructionID())) == null) {
                if (server != null && server.isConnected()) {
                    server.sendTCP(new PacketGetInstruction(work.getInstructionID()));
                }
            } else {
                try {
                    System.out.println("Parameter number: " + work.getParametersNumber());
                    System.out.println(instruction.getDeclaredMethods()[0].invoke(null));
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void stop() {
        client.stop();
        client.close();
        running = false;
    }

    private synchronized void cleanUp(String msg) {
        stop();
        System.err.println(msg);
    }

}
