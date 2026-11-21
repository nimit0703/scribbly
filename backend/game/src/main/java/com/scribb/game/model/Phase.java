package com.scribb.game.model;

/** Room lifecycle. CHOOSING, DRAWING and TURN_END only happen while a game is running. */
public enum Phase {
    LOBBY, CHOOSING, DRAWING, TURN_END, GAME_OVER;

    public boolean inGame() {
        return this == CHOOSING || this == DRAWING || this == TURN_END;
    }
}
