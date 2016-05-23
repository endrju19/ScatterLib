package com.scatterlib.packets;

/**
 * Created by przemek on 23.05.16.
 */
public class PacketGetInstruction {

    private byte instructionID;


    public PacketGetInstruction() {
    }

    public PacketGetInstruction(byte instructionID) {
        this.instructionID = instructionID;
    }

    public byte getInstructionID() {
        return instructionID;
    }
}

