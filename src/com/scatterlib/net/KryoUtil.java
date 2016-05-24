package com.scatterlib.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import com.scatterlib.packets.*;

import java.util.ArrayList;

/**
 * Created by przemek on 21.05.16.
 */
public class KryoUtil {

    public static final int TCP_PORT = 11155;
    public static final int UDP_PORT = 11155;

    public static void registerServerClasses(Server server) {
        register(server.getKryo());
    }

    public static void registerClientClass(Client client) {
        register(client.getKryo());
    }

    private static void register(Kryo kryo) {
        kryo.register(byte.class);
        kryo.register(boolean.class);
        kryo.register(boolean[].class);
        kryo.register(int.class);
        kryo.register(String.class);
        kryo.register(Short.class);
        kryo.register(ArrayList.class);

        kryo.register(PacketData.class);
        kryo.register(PacketGetInstruction.class);
        kryo.register(PacketGetWork.class);
        kryo.register(PacketInstruction.class);
        kryo.register(PacketJoinRequest.class);
        kryo.register(PacketJoinResponse.class);
        kryo.register(PacketNoNeed.class);
        kryo.register(PacketResult.class);
        kryo.register(PacketWork.class);
    }
}
