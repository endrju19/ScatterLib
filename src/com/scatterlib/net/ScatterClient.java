package com.scatterlib.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.scatterlib.mdkt.compiler.InMemoryJavaCompiler;
import com.scatterlib.packets.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterClient {

    private final Client client;
    private Connection server;
    private Thread[] threads;
    private boolean running;
    private long delay = 1000;

    private long lastTime[];
    private boolean askedForInstrucion;
    private PacketWork[] works;

    private Map<Byte, Class> instructions = new HashMap<>();
    private ArrayList<PacketData>[] data;
    private boolean[] askedForWork, haveWork;


    public ScatterClient(String IP, int threadsCount) {
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
            Listener listener = new Listener() {
                @Override
                public void received(Connection connection, Object obj) {
                    if (obj instanceof PacketData) {
                        data[((PacketData) obj).getThreadID()].add((PacketData) obj);
                    } else if (obj instanceof PacketWork) {
                        PacketWork packet = (PacketWork) obj;
                        if (!haveWork[packet.getThreadID()]) {
                            works[packet.getThreadID()] = packet;
                            data[packet.getThreadID()].clear();
                            haveWork[packet.getThreadID()] = true;
                        } else {
                            connection.sendTCP(new PacketNoNeed(packet.getParametersID()));
                        }
                    } else if (obj instanceof PacketInstruction) {
                        askedForInstrucion = false;
                        addInstruction((PacketInstruction) obj);
                        System.out.println("Instruction get");
                    } else if (obj instanceof PacketJoinResponse) {
                        server = connection;
                        System.out.println("My ID: " + ((PacketJoinResponse) obj).getID());
                    } else if (obj instanceof PacketDone) {
                        System.out.println("All Done. Shutting down...");
                        stop();
                    }
                }

                @Override
                public void connected(Connection connection) {
                    PacketJoinRequest join = new PacketJoinRequest();
                    client.sendTCP(join);
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

    private synchronized void addInstruction(PacketInstruction instruction) {
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

    public synchronized void start(int threadsCount) {
        running = true;
        threads = new Thread[threadsCount];
        data = new ArrayList[threadsCount];
        askedForWork = new boolean[threadsCount];
        haveWork = new boolean[threadsCount];
        works = new PacketWork[threadsCount];
        lastTime = new long[threadsCount];
        long time = System.currentTimeMillis();
        for (byte i = 0; i < lastTime.length; i++) {
            lastTime[i] = time;
            data[i] = new ArrayList<>();
        }
        for (byte i = 0; i < threadsCount; i++) {
            final byte threadID = i;
            threads[i] = new Thread(() -> {
                while (running) {
                    if (haveWork[threadID]) {
                        work(threadID);
                    } else {
                        if (System.currentTimeMillis() - lastTime[threadID] > delay || !askedForWork[threadID]) {
                            if (server != null && server.isConnected()) {
                                lastTime[threadID] = System.currentTimeMillis();
                                getWork(threadID);
                                askedForWork[threadID] = true;
                            }
                        }
                    }
                }
            });
            threads[i].start();
        }
    }

    private synchronized void getWork(byte threadID) {
        PacketGetWork getWork = new PacketGetWork(threadID);
        server.sendTCP(getWork);
    }

    public synchronized void work(byte threadID) {
        if (works[threadID] != null) {
            Class<?> instruction;
            if ((instruction = instructions.get(works[threadID].getInstructionID())) == null) {
                if (server != null && server.isConnected() && !askedForInstrucion) {
                    server.sendTCP(new PacketGetInstruction(works[threadID].getInstructionID()));
                    askedForInstrucion = true;
                }
            } else {
                try {
                    if (data[threadID].size() == works[threadID].getParametersNumber()) {
                        Object result = null;
                        Method method = null;
                        if (instruction.getDeclaredMethods().length > works[threadID].getMethodID()) {
                            method = instruction.getDeclaredMethods()[works[threadID].getMethodID()];
                        }
                        if (method != null) {
                            switch (data[threadID].size()) {
                                case 0:
                                    result = method.invoke(null);
                                    break;
                                case 1:
                                    result = method.invoke(null, data[threadID].get(0).getData());
                                    break;
                                case 2:
                                    result = method.invoke(null, data[threadID].get(0).getData(), data[threadID].get(1).getData());
                                    break;
                                case 3:
                                    result = method.invoke(null, data[threadID].get(0).getData(), data[threadID].get(1).getData(), data[threadID].get(2).getData());
                                    break;
                            }
                            System.out.println(result);
                            if (result != null) {
                                server.sendTCP(new PacketResult(result, works[threadID].getParametersID()));
                                haveWork[threadID] = false;
                            }
                        }
                    }
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
