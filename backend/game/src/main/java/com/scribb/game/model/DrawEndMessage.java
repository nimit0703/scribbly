package com.scribb.game.model;

public class DrawEndMessage {
    private String roomId;
    private String username;

    public DrawEndMessage() {}

    public DrawEndMessage(String roomId, String username) {
        this.roomId = roomId;
        this.username = username;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
