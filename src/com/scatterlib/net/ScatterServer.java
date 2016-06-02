package com.scatterlib.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.scatterlib.packets.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by przemek on 21.05.16.
 */
public class ScatterServer {

    public final static int SUM = 0, MULTIPLY = 1, TO_ARRAY = 2;
    private final Server server;
    private Thread thread;
    private byte ID = 0;
    private byte taskID = 0;
    private int parametersID = 0;
    private ArrayList<Class> classes = new ArrayList<>();
    private Map<Byte, PacketInstruction> instructions = new HashMap<>();
    private int combination;
    private Class<?> resultType;
    private Object[] data;
    private Object sum;
    private Object[] array;
    private int size, chunksSize, currentResult;
    private boolean allDone = false;

    public ScatterServer(int combination, Object[] data, int size, Class<?> resultType) {
        initialize(combination, data, size, resultType);
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
                        connection.sendTCP(instructions.get((byte) 0));
                    } else if (obj instanceof PacketGetWork) {
                        if (parametersID < size) {
                            PacketInstruction inst = instructions.get((byte) 0);
                            connection.sendTCP(new PacketWork(inst.getID(), 0, parametersID, 1));
                            int start = parametersID * chunksSize;
                            int end = start + chunksSize;
                            if (end > data.length) {
                                end = data.length;
                            }
                            Object[] subArray = Arrays.copyOfRange(data, parametersID * chunksSize, end);
                            connection.sendTCP(new PacketData(subArray));
                            parametersID++;
                        } else {
                            if (allDone) {
//                            TODO send signal to stop work.
                                System.out.println("All done");
                                connection.close();
                            } else {
                                allDone = true;
                            }
                        }
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

    private void initialize(int combination, Object[] data, int size, Class<?> resultType) {
        this.combination = combination;
        this.data = data;
        this.size = size;
        this.resultType = resultType;
        chunksSize = data.length / size;
        if (chunksSize * size != data.length) {
            chunksSize++;
        }
        if (combination == TO_ARRAY) {
            array = new Object[size];
        }
        if (combination < TO_ARRAY) {
            if (resultType == Integer.class) {
                this.sum = new Integer(combination == SUM ? 0 : 1);
            } else if (resultType == Float.class) {
                this.sum = new Float(combination == SUM ? 0 : 1);
            } else if (resultType == Double.class) {
                this.sum = new Double(combination == SUM ? 0 : 1);
            } else if (resultType == BigDecimal.class) {
                this.sum = new BigDecimal(combination == SUM ? 0 : 1);
            } else if (resultType == Byte.class) {
                this.sum = new Byte(combination == SUM ? (byte) 0 : (byte) 1);
            } else if (resultType == Short.class) {
                this.sum = new Short(combination == SUM ? (short) 0 : (short) 1);
            } else if (resultType == Long.class) {
                this.sum = new Long(combination == SUM ? 0 : 1);
            }
        }
    }

    private void combineResult(PacketResult result) {
//        currentResult++;
        switch (combination) {
            case SUM:
                sumData(result.getResult());
                break;
            case MULTIPLY:
                multiplyData(result.getResult());
                break;
            case TO_ARRAY:
                if (result.getParametersID() < size) {
                    array[result.getParametersID()] = result.getResult();
                } else {
                    System.out.println("Too big result id!");
                }
                break;
        }
        System.out.println("Current result: " + sum);
    }

    private void sumData(Object result) {
        if (result instanceof Integer) {
            sum = (int) result + (int) sum;
        } else if (result instanceof Float) {
            sum = (float) result + (float) sum;
        } else if (result instanceof Double) {
            sum = (double) result + (double) sum;
        } else if (result instanceof String) {
            sum = result + sum;
        } else if (result instanceof BigDecimal) {
            sum = ((BigDecimal) result).add((BigDecimal) sum);
        } else if (result instanceof Byte) {
            sum = (byte) result + (byte) sum;
        } else if (result instanceof Short) {
            sum = (short) result + (short) sum;
        } else if (result instanceof Long) {
            sum = (long) result + (long) sum;
        }
    }

    private void multiplyData(Object result) {
        if (result instanceof Integer) {
            sum = (int) result * (int) sum;
        } else if (result instanceof Float) {
            sum = (float) result * (float) sum;
        } else if (result instanceof Double) {
            sum = (double) result * (double) sum;
        } else if (result instanceof BigDecimal) {
            sum = ((BigDecimal) result).multiply((BigDecimal) sum);
        } else if (result instanceof Byte) {
            sum = (byte) result * (byte) sum;
        } else if (result instanceof Short) {
            sum = (short) result * (short) sum;
        } else if (result instanceof Long) {
            sum = (long) result * (long) sum;
        }
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
