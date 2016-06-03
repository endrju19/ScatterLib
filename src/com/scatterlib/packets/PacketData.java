package com.scatterlib.packets;

public class PacketData {

    Object piece;
    private byte threadID;

    public PacketData() {
    }

    public PacketData(Object piece, byte threadID) {
        this.piece = piece;
        this.threadID = threadID;
    }

    public Object getData() {
        return piece;
    }

    public byte getThreadID() {
        return threadID;
    }
}
