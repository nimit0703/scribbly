package com.scribb.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
    private final String id;
    @JsonIgnore
    private final String sessionId;
    private final String name;
    private final Avatar avatar;
    private int score;
    /** True once this player guessed the current word (reset every turn). */
    private boolean guessed;

    public Player(String id, String sessionId, String name, Avatar avatar) {
        this.id = id;
        this.sessionId = sessionId;
        this.name = name;
        this.avatar = avatar;
    }
}
