package com.scribb.game.model;

import lombok.Data;

@Data
public class DrawMessage {
    private String roomId;
    private int x;
    private int y;
    private String color;
    private int width;
    private boolean isDragging;
    private String  username;
}
