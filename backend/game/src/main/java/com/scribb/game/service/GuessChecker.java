package com.scribb.game.service;

import java.util.Locale;

/** Decides whether a chat message is a correct or a close guess. */
public final class GuessChecker {
    private GuessChecker() {}

    /** Lower-case, letters and digits only: "Ice-Cream " -> "icecream". */
    public static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    public static boolean isCorrect(String guess, String word) {
        String w = normalize(word);
        return !w.isEmpty() && normalize(guess).equals(w);
    }

    /** One typo away (insert, delete or substitute a letter). Short words are too easy to brute-force, so never "close". */
    public static boolean isClose(String guess, String word) {
        String g = normalize(guess);
        String w = normalize(word);
        if (w.length() < 4 || g.equals(w) || Math.abs(g.length() - w.length()) > 1) return false;
        return levenshtein(g, w) == 1;
    }

    /** True when the message would give the word away (used to stop the drawer from typing it). */
    public static boolean revealsWord(String text, String word) {
        String w = normalize(word);
        return !w.isEmpty() && normalize(text).contains(w);
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
