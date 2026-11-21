package com.scribb.game.model;

import java.util.List;

/** Public snapshot of a room, broadcast to everyone in it. Never contains the secret word while it is being guessed. */
public record RoomView(
        String code,
        boolean isPrivate,
        String ownerId,
        Phase phase,
        RoomSettings settings,
        int round,
        String drawerId,
        String hint,
        int timeLeft,
        List<Player> players,
        TurnResult turnResult) {
}
