package com.scribb.game.model;

/**
 * One drawing operation on the 800x600 logical canvas.
 * t = "s" stroke: p is a polyline [x0,y0,x1,y1,...], s is brush size.
 * t = "f" flood fill: p is [x,y].
 * g groups the ops of one pen-down..pen-up gesture so undo can remove it as a unit.
 */
public record DrawOp(String t, int g, String c, int s, int[] p) {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    private static final int MAX_POINTS = 500;

    /** Returns a clamped copy, or null when the op is malformed. */
    public static DrawOp sanitize(DrawOp op) {
        if (op == null || op.p == null || op.c == null || !op.c.matches("#[0-9a-fA-F]{6}")) return null;
        boolean stroke = "s".equals(op.t);
        if (!stroke && !"f".equals(op.t)) return null;
        int len = op.p.length;
        if (len < 2 || len % 2 != 0 || len > MAX_POINTS * 2 || (!stroke && len != 2)) return null;
        int[] p = new int[len];
        for (int i = 0; i < len; i += 2) {
            p[i] = Math.max(0, Math.min(WIDTH, op.p[i]));
            p[i + 1] = Math.max(0, Math.min(HEIGHT, op.p[i + 1]));
        }
        return new DrawOp(op.t, op.g, op.c.toLowerCase(), Math.max(1, Math.min(60, op.s)), p);
    }
}
