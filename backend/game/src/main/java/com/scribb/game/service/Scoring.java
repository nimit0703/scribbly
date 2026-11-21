package com.scribb.game.service;

/**
 * Points rules (see documents/GAME_RULES.md):
 * a guesser gets 50 + up to 350 for speed, plus 100/50/25 for being 1st/2nd/3rd.
 * The drawer gets half of every correct guesser's points, so a drawing many people get pays off.
 */
public final class Scoring {
    private Scoring() {}

    public static int guesser(long timeLeftMs, long drawTimeMs, int order) {
        double frac = drawTimeMs <= 0 ? 0 : Math.max(0, Math.min(1, (double) timeLeftMs / drawTimeMs));
        int orderBonus = switch (order) {
            case 0 -> 100;
            case 1 -> 50;
            case 2 -> 25;
            default -> 0;
        };
        return 50 + (int) Math.round(350 * frac) + orderBonus;
    }

    public static int drawerShare(int guesserPoints) {
        return guesserPoints / 2;
    }
}
