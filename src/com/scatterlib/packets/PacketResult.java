package com.scatterlib.packets;

/**
 * Created by przemek on 23.05.16.
 */
public class PacketResult {

    private Object result;
    private int parametersID;

    public PacketResult() {
    }

    public PacketResult(Object result, int parametersID) {
        this.result = result;
        this.parametersID = parametersID;
    }

    public Object getResult() {
        return result;
    }

    public int getParametersID() {
        return parametersID;
    }
}
