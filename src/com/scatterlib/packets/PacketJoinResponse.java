package com.scatterlib.packets;

/**
 * Created by przemek on 21.05.16.
 */
public class PacketJoinResponse {

    private byte ID;

    public PacketJoinResponse() {
    }

    public PacketJoinResponse(byte ID) {
        this.ID = ID;
    }

    public synchronized byte getID() {
        return ID;
    }
}
