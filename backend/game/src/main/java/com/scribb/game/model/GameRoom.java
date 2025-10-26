package com.scribb.game.model;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Data;

@Data
public class GameRoom {
    private String roomId;
    private List<Player> players = new CopyOnWriteArrayList<>();
    private String currentWord;
    private String currentDrawer;
    private int roundNumber = 0;
    private int lastDrawerIndex = -1;
    private long roundStartTime;
    private long timerStartTime;
    private boolean timerActive = false;
    private int roundDurationMillis = 60000;
    private Set<String> correctGuessers = new HashSet<>();
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private long lastActivityTime;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public Optional<Player> getPlayer(String username) {
        return players.stream()
            .filter(p -> p.getUsername().equals(username))
            .findFirst();
    }

    public void addPlayer(Player player) {
        if (getPlayer(player.getUsername()).isEmpty()) {
            players.add(player);
            this.lastActivityTime = System.currentTimeMillis();
        }
    }

    public void removePlayer(String username) {
        players.removeIf(p -> p.getUsername().equals(username));
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void resetForNextRound() {
        this.currentWord = null;
        this.correctGuessers.clear();
        
        // Reset player guess status
        players.forEach(p -> p.setHasGuessedCorrectly(false));
        
        // Increment round number
        this.roundNumber++;
        
        // Check if game is over (you can customize this based on total rounds)
        if (this.roundNumber > 3) { // Default 3 rounds
            this.gameOver = true;
        }
        
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void startTimer() {
        this.timerActive = true;
        this.timerStartTime = System.currentTimeMillis();
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void stopTimer() {
        this.timerActive = false;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public int getRemainingTimeSeconds() {
        if (!timerActive) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - timerStartTime;
        long remaining = roundDurationMillis - elapsed;
        return (int) Math.max(0, remaining / 1000);
    }
}