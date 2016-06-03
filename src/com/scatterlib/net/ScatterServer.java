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
    public final static byte TO_DO = 0, SEND = 1, DONE = 2;
    private final Server server;
    private Thread thread;
    private byte ID = 0;
    private byte taskID = 0;
    private int parametersID = 0;
    private ArrayList<Class> classes = new ArrayList<>();
    private Map<Byte, PacketInstruction> instructions = new HashMap<>();
    private int combination;
    private Object[] data;
    private Object sum;
    private Object[] array;
    private int size, chunksSize;
    private boolean allDone = false;
    private byte[] statuses;

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
            Log.set(Log.LEVEL_DEBUG);
            KryoUtil.registerServerClasses(server);
            Listener listener = new Listener() {

                @Override
                public synchronized void received(Connection connection, Object obj) {
                    if (obj instanceof PacketGetInstruction) {
                        connection.sendTCP(instructions.get((byte) 0));
                    } else if (obj instanceof PacketGetWork) {
                        if (parametersID <= size || anyWorkLeft()) {
                            if (parametersID < size) {
                                sendWord(connection, instructions.get((byte) 0).getID(), parametersID, ((PacketGetWork) obj).getThreadID());
                            } else {
                                int missingID = getMissingDataID();
                                if (missingID >= 0) {
                                    sendWord(connection, instructions.get((byte) 0).getID(), missingID, ((PacketGetWork) obj).getThreadID());
                                }
                            }
                        } else {
                            if (allDone) {
                                connection.sendTCP(new PacketDone());
                                System.out.println(getResult());
                            } else {
                                allDone = true;
                            }
                        }
                    } else if (obj instanceof PacketResult) {
                        combineResult((PacketResult) obj);
                    } else if (obj instanceof PacketJoinRequest) {
                        connection.sendTCP(new PacketJoinResponse(ID++));
                    } else if (obj instanceof PacketNoNeed) {
                        statuses[((PacketNoNeed) obj).getParametersID()] = TO_DO;
                    }
                }

                @Override
                public synchronized void connected(Connection connection) {
                    System.out.println("Received a connection from " + connection.getRemoteAddressTCP().getHostString() + " (" + connection.getID() + ")");
                }

                @Override
                public synchronized void disconnected(Connection connection) {
                    System.out.println(connection.toString() + " disconnected.");
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


    private void sendWord(Connection connection, byte instID, int paramID, byte threadID) {
        connection.sendTCP(new PacketWork(instID, 0, paramID, 1, threadID));
        int start = paramID * chunksSize;
        int end = start + chunksSize;
        if (end > data.length) {
            end = data.length;
        }
        Object[] subArray = Arrays.copyOfRange(data, paramID * chunksSize, end);
        connection.sendTCP(new PacketData(subArray, threadID));
        statuses[paramID] = SEND;
        parametersID++;
    }

    private boolean anyWorkLeft() {
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i] != DONE) {
                return true;
            }
        }
        return false;
    }

    private void initialize(int combination, Object[] data, int size, Class<?> resultType) {
        this.combination = combination;
        this.data = data;
        this.size = size;
        chunksSize = data.length / size;
        if (chunksSize * size != data.length) {
            chunksSize++;
        }
        if (combination == TO_ARRAY) {
            array = new Object[size];
        }
        statuses = new byte[size];
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
        if (statuses[result.getParametersID()] != DONE) {
            statuses[result.getParametersID()] = DONE;
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
                        System.out.println("Too big result ID!");
                    }
                    break;
            }
        }
    }

    private void sumData(Object result) {
        if (result instanceof Integer) {
            sum = (int) result + (int) sum;
        } else if (result instanceof Float) {
            sum = (float) result + (float) sum;
        } else if (result instanceof Double) {
            sum = (double) result + (double) sum;
        } else if (result instanceof String) {
            sum = (String) result + (String) sum;
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


    public synchronized Object getResult() {
        if (isAllDone()) {
            switch (combination) {
                case SUM:
                case MULTIPLY:
                    return sum;
                case TO_ARRAY:
                    return array;
            }
        }
        return null;
    }


    public boolean isAllDone() {
        return allDone;
    }

    public int getMissingDataID() {
        for (int i = 0; i < size; i++) {
            if (statuses[i] != DONE) {
                return i;
            }
        }
        return -1;
    }
}
