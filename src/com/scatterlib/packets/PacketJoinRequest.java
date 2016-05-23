package com.scatterlib.packets;


public class PacketJoinRequest {

    private String message;

    public PacketJoinRequest() {
    }

    public PacketJoinRequest(String message) {
        this.message = message;
    }

    public synchronized String getMessage() {
        return message;
    }
}
