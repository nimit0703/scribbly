package com.scribb.game.model;

public class DrawStartMessage {
    private String roomId;
    private String username;
    private double x;
    private double y;

    // Constructors, getters, setters
    public DrawStartMessage() {}

    public DrawStartMessage(String roomId, String username, double x, double y) {
        this.roomId = roomId;
        this.username = username;
        this.x = x;
        this.y = y;
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
