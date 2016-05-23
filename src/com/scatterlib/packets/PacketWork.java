package com.scatterlib.packets;

public class PacketWork {

    private byte instructionID;
    private byte parametersNumber;


    public PacketWork() {
    }

    public PacketWork(byte instructionID, byte parametersNumber) {
        this.instructionID = instructionID;
        this.parametersNumber = parametersNumber;
    }

    public byte getInstructionID() {
        return instructionID;
    }

    public byte getParametersNumber() {
        return parametersNumber;
    }
}
