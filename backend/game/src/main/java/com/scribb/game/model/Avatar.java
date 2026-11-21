package com.scribb.game.model;

/** Indexes into the client's avatar parts (body colour, eyes, mouth). The client wraps them with modulo. */
public record Avatar(int color, int eyes, int mouth) {
    public static Avatar sanitize(Avatar a) {
        if (a == null) return new Avatar(0, 0, 0);
        return new Avatar(clamp(a.color), clamp(a.eyes), clamp(a.mouth));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(63, v));
    }
}
