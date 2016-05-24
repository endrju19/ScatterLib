package com.scatterlib.packets;

public class PacketData {

    Object piece;

    public PacketData() {
    }

    public PacketData(Object piece) {
        this.piece = piece;
    }

    public Object getData() {
        return piece;
    }

}
