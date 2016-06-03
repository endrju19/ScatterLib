package com.scatterlib.packets;

/**
 * Created by przemek on 23.05.16.
 */
public class PacketGetWork {

    private byte threadID;

    public PacketGetWork() {
    }

    public PacketGetWork(byte threadID) {
        this.threadID = threadID;
    }

    public byte getThreadID() {
        return threadID;
    }
}
