package com.scribb.game.service.monitoring;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.scribb.game.model.matrics.GameMetrics;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MonitoringService {

    private final GameMetrics gameMetrics;
    private final ConcurrentMap<String, RoomMetrics> roomMetrics;
    private final ConcurrentMap<String, Instant> roundStartTimes;
    private final ConcurrentMap<String, Instant> drawingStartTimes;

    public MonitoringService(GameMetrics gameMetrics) {
        this.gameMetrics = gameMetrics;
        this.roomMetrics = new ConcurrentHashMap<>();
        this.roundStartTimes = new ConcurrentHashMap<>();
        this.drawingStartTimes = new ConcurrentHashMap<>();
    }

    // Player tracking
    public void trackPlayerJoin(String roomId, String username) {
        log.info("Player {} joined room {}", username, roomId);
        roomMetrics.computeIfAbsent(roomId, k -> new RoomMetrics(roomId))
                  .incrementPlayerCount();
    }

    public void trackPlayerLeave(String roomId, String username) {
        log.info("Player {} left room {}", username, roomId);
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.decrementPlayerCount();
            if (metrics.getPlayerCount() == 0) {
                roomMetrics.remove(roomId);
            }
        }
    }

    // Game round tracking
    public void trackRoundStart(String roomId) {
        log.info("Round started in room {}", roomId);
        roundStartTimes.put(roomId, Instant.now());
        roomMetrics.computeIfAbsent(roomId, k -> new RoomMetrics(roomId))
                  .incrementRoundsStarted();
    }

    public void trackRoundEnd(String roomId, String endReason) {
        log.info("Round ended in room {}: {}", roomId, endReason);
        Instant startTime = roundStartTimes.remove(roomId);
        if (startTime != null) {
            long duration = Instant.now().getEpochSecond() - startTime.getEpochSecond();
            gameMetrics.recordRoundDuration(duration, TimeUnit.SECONDS);
        }

        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementRoundsCompleted();
        }
    }

    public void trackRoundCompletion(String roomId, String completionType) {
        trackRoundEnd(roomId, completionType);
    }

    // Drawing tracking
    public void trackDrawingActivity(String roomId, String username) {
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementDrawEvents();
        }
    }

    public void trackDrawingCompletion(String roomId, String username) {
        Instant startTime = drawingStartTimes.remove(roomId + ":" + username);
        if (startTime != null) {
            long duration = Instant.now().getEpochSecond() - startTime.getEpochSecond();
            gameMetrics.recordDrawingSessionTime(duration, TimeUnit.SECONDS);
        }
    }

    public void startDrawingSession(String roomId, String username) {
        drawingStartTimes.put(roomId + ":" + username, Instant.now());
    }

    // Guess tracking
    public void trackCorrectGuess(String roomId, String username, int pointsAwarded) {
        log.info("Player {} guessed correctly in room {}, awarded {} points", username, roomId, pointsAwarded);
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementCorrectGuesses();
            metrics.addPointsAwarded(pointsAwarded);
        }
    }

    public void trackIncorrectGuess(String roomId, String username) {
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementIncorrectGuesses();
        }
    }

    // Word selection tracking
    public void trackWordSelection(String roomId, String username, String word) {
        log.info("Player {} selected word '{}' in room {}", username, word, roomId);
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementWordsSelected();
        }
    }

    // Chat tracking
    public void trackChatMessage(String roomId, String username) {
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementChatMessages();
        }
    }

    // Hint tracking
    public void trackHintRequest(String roomId, String username) {
        log.info("Player {} requested hint in room {}", username, roomId);
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementHintRequests();
        }
    }

    // Error tracking
    public void trackError(String roomId, String errorType, Exception exception) {
        log.error("Error in room {}: {} - {}", roomId, errorType, exception.getMessage());
        RoomMetrics metrics = roomMetrics.get(roomId);
        if (metrics != null) {
            metrics.incrementErrors();
        }
    }

    // Performance tracking
    public void startGuessTimer() {
        // This would be used to time guess processing
        // Implementation depends on your specific timing needs
    }

    public void recordGuessProcessingTime(long duration, TimeUnit unit) {
        gameMetrics.recordGuessResponseTime(duration, unit);
    }

    // Analytics methods
    public RoomMetrics getRoomMetrics(String roomId) {
        return roomMetrics.get(roomId);
    }

    public ConcurrentMap<String, RoomMetrics> getAllRoomMetrics() {
        return new ConcurrentHashMap<>(roomMetrics);
    }

    public int getTotalActiveRooms() {
        return (int) roomMetrics.values().stream()
                .filter(metrics -> metrics.getPlayerCount() > 0)
                .count();
    }

    public int getTotalActivePlayers() {
        return roomMetrics.values().stream()
                .mapToInt(RoomMetrics::getPlayerCount)
                .sum();
    }

    // RoomMetrics inner class
    public static class RoomMetrics {
        private final String roomId;
        private final Instant createdAt;
        private int playerCount;
        private int roundsStarted;
        private int roundsCompleted;
        private int correctGuesses;
        private int incorrectGuesses;
        private int wordsSelected;
        private int drawEvents;
        private int chatMessages;
        private int hintRequests;
        private int errors;
        private int pointsAwarded;

        public RoomMetrics(String roomId) {
            this.roomId = roomId;
            this.createdAt = Instant.now();
        }

        // Getters
        public String getRoomId() { return roomId; }
        public Instant getCreatedAt() { return createdAt; }
        public int getPlayerCount() { return playerCount; }
        public int getRoundsStarted() { return roundsStarted; }
        public int getRoundsCompleted() { return roundsCompleted; }
        public int getCorrectGuesses() { return correctGuesses; }
        public int getIncorrectGuesses() { return incorrectGuesses; }
        public int getWordsSelected() { return wordsSelected; }
        public int getDrawEvents() { return drawEvents; }
        public int getChatMessages() { return chatMessages; }
        public int getHintRequests() { return hintRequests; }
        public int getErrors() { return errors; }
        public int getPointsAwarded() { return pointsAwarded; }

        // Increment methods
        public void incrementPlayerCount() { playerCount++; }
        public void decrementPlayerCount() { playerCount = Math.max(0, playerCount - 1); }
        public void incrementRoundsStarted() { roundsStarted++; }
        public void incrementRoundsCompleted() { roundsCompleted++; }
        public void incrementCorrectGuesses() { correctGuesses++; }
        public void incrementIncorrectGuesses() { incorrectGuesses++; }
        public void incrementWordsSelected() { wordsSelected++; }
        public void incrementDrawEvents() { drawEvents++; }
        public void incrementChatMessages() { chatMessages++; }
        public void incrementHintRequests() { hintRequests++; }
        public void incrementErrors() { errors++; }
        public void addPointsAwarded(int points) { pointsAwarded += points; }

        public double getGuessAccuracy() {
            int totalGuesses = correctGuesses + incorrectGuesses;
            return totalGuesses > 0 ? (double) correctGuesses / totalGuesses : 0.0;
        }

        public double getRoundCompletionRate() {
            return roundsStarted > 0 ? (double) roundsCompleted / roundsStarted : 0.0;
        }
    }
}