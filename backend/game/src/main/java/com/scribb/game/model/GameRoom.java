package com.scribb.game.model;

import lombok.*;

import java.util.ArrayList;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameRoom {
    private String roomId;
    private List<Player> players = new ArrayList<>();
    private String currentWord;
    private String currentDrawer;
    private boolean gameStarted = false;
    private int roundNumber = 1;
    private final int totalRounds = 10;
    private long roundStartTime;
    private Set<String> correctGuessers = new HashSet<>();
    private int lastDrawerIndex = -1;
    public GameRoom(String roomId) {
        this.roomId = roomId;
    }
    private final long roundDurationMillis = 60_000; // 60 seconds

    public void setCurrentWord(String currentWord) {
        this.currentWord = currentWord;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public Optional<Player> getPlayer(String username) {
        return players.stream().filter(p -> p.getUsername().equals(username)).findFirst();
    }
    public int getLastDrawerIndex() {
        return lastDrawerIndex;
    }

    public void setLastDrawerIndex(int index) {
        this.lastDrawerIndex = index;
    }
    public void resetForNextRound() {
        currentWord = null;
        currentDrawer = null;
        correctGuessers.clear();
        for (Player p : players) {
            p.setHasGuessedCorrectly(false);
        }
        roundNumber++;
    }

    public boolean isGameOver() {
        return roundNumber > totalRounds;
    }
}
