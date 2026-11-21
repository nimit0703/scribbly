package com.scribb.game.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Getter;
import lombok.Setter;

/**
 * Mutable state of one room. Every read-modify-write must hold the room's monitor
 * (synchronized (room)); GameEngine does that for all entry points.
 */
@Getter
@Setter
public class GameRoom {
    private final String code;
    private final boolean isPrivate;
    private RoomSettings settings;
    private String ownerId;
    private boolean closed;

    /** CopyOnWrite so matchmaking can read the size without taking the lock. */
    private final List<Player> players = new CopyOnWriteArrayList<>();

    private Phase phase = Phase.LOBBY;
    private int round;
    /** Players still to draw in this round, in order. */
    private Deque<String> drawQueue = new ArrayDeque<>();
    private String drawerId;
    private List<String> wordChoices = List.of();
    private String word;
    private final Set<Integer> revealed = new HashSet<>();
    /** Epoch millis at which the next hint letters are revealed. */
    private final Deque<Long> hintTimes = new ArrayDeque<>();
    /** Epoch millis when the current timed phase ends. */
    private long deadline;
    private int guessedCount;
    private final Map<String, Integer> gains = new LinkedHashMap<>();
    private final List<DrawOp> ops = new ArrayList<>();
    private final Map<String, Set<String>> kickVotes = new HashMap<>();
    private final Set<String> raters = new HashSet<>();
    private TurnResult turnResult;

    public GameRoom(String code, boolean isPrivate, RoomSettings settings) {
        this.code = code;
        this.isPrivate = isPrivate;
        this.settings = settings;
    }

    public Player player(String id) {
        for (Player p : players) if (p.getId().equals(id)) return p;
        return null;
    }

    public Player drawer() {
        return drawerId == null ? null : player(drawerId);
    }

    /** Clears everything that belongs to a single turn. */
    public void resetTurn() {
        word = null;
        wordChoices = List.of();
        revealed.clear();
        hintTimes.clear();
        guessedCount = 0;
        gains.clear();
        ops.clear();
        raters.clear();
        turnResult = null;
        players.forEach(p -> p.setGuessed(false));
    }
}
