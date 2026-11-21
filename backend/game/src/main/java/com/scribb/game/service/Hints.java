package com.scribb.game.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Pure helpers for the "_ _ a _" hint line. */
public final class Hints {
    private Hints() {}

    /** Letters become '_' unless revealed; spaces, hyphens and other punctuation stay visible. */
    public static String mask(String word, Set<Integer> revealed) {
        StringBuilder sb = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            sb.append(!Character.isLetterOrDigit(c) || revealed.contains(i) ? c : '_');
        }
        return sb.toString();
    }

    public static int letterCount(String word) {
        int n = 0;
        for (int i = 0; i < word.length(); i++) if (Character.isLetterOrDigit(word.charAt(i))) n++;
        return n;
    }

    /** Never reveal more than half the letters, so the word still has to be guessed. */
    public static int hintCount(String word, int configured) {
        return Math.max(0, Math.min(configured, letterCount(word) / 2));
    }

    /** Evenly spaced reveal times inside the drawing window, e.g. 2 hints in 90s -> at 30s and 60s. */
    public static List<Long> schedule(long start, long durationMs, int count) {
        List<Long> times = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) times.add(start + durationMs * i / (count + 1));
        return times;
    }

    /** A random unrevealed letter index, or -1 when every letter is visible. */
    public static int pickNext(String word, Set<Integer> revealed, Random random) {
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetterOrDigit(word.charAt(i)) && !revealed.contains(i)) hidden.add(i);
        }
        return hidden.isEmpty() ? -1 : hidden.get(random.nextInt(hidden.size()));
    }
}
