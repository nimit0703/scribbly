package com.scribb.game.model;

import lombok.Data;

@Data
public class JoinRequest {
    private String roomId;
    private String username;
}
