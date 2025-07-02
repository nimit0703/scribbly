package com.scribb.game.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String roomId;
    private String username;
    private String content;
}
