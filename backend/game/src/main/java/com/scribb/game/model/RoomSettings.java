package com.scribb.game.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.Data;

/** Settings the owner of a private room can change in the lobby. */
@Data
public class RoomSettings {
    public static final int MAX_CUSTOM_WORDS = 500;

    private int maxPlayers = 8;
    private int rounds = 3;
    private int drawTime = 80;
    private int wordCount = 3;
    private int hints = 2;
    private List<String> customWords = new ArrayList<>();
    private boolean customOnly = false;

    /** Returns a copy with every value clamped to its allowed range. Untrusted input goes through here. */
    public RoomSettings sanitized(int currentPlayers) {
        RoomSettings s = new RoomSettings();
        s.maxPlayers = clamp(maxPlayers, Math.max(2, currentPlayers), 12);
        s.rounds = clamp(rounds, 1, 10);
        s.drawTime = clamp(drawTime, 15, 240);
        s.wordCount = clamp(wordCount, 1, 5);
        s.hints = clamp(hints, 0, 5);
        var words = new LinkedHashSet<String>();
        if (customWords != null) {
            for (String w : customWords) {
                if (w == null) continue;
                String t = w.trim().toLowerCase().replaceAll("\\s+", " ");
                if (t.length() >= 1 && t.length() <= 30 && words.size() < MAX_CUSTOM_WORDS) words.add(t);
            }
        }
        s.customWords = new ArrayList<>(words);
        s.customOnly = customOnly;
        return s;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
