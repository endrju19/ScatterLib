package com.scatterlib.packets;

/**
 * Created by przemek on 23.05.16.
 */
public class PacketInstruction {

    private byte ID;
    private String name;
    private String work;

    public PacketInstruction() {
    }

    public PacketInstruction(byte ID, String name, String work) {
        this.ID = ID;
        this.name = name;
        this.work = work;
    }

    public synchronized String getWork() {
        return work;
    }

    public synchronized String getName() {
        return name;
    }

    public byte getID() {
        return ID;
    }
}
