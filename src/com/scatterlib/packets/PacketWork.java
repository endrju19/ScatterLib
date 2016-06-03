package com.scatterlib.packets;

public class PacketWork {

    private byte instructionID;
    private int parametersNumber;
    private int parametersID;
    private int methodID;
    private byte threadID;


    public PacketWork() {
    }

    public PacketWork(byte instructionID, int methodID, int parametersID, int parametersNumber, byte threadID) {
        this.instructionID = instructionID;
        this.methodID = methodID;
        this.parametersID = parametersID;
        this.parametersNumber = parametersNumber;
        this.threadID = threadID;
    }

    public byte getInstructionID() {
        return instructionID;
    }

    public int getParametersNumber() {
        return parametersNumber;
    }

    public int getParametersID() {
        return parametersID;
    }

    public int getMethodID() {
        return methodID;
    }

    public byte getThreadID() {
        return threadID;
    }
}
