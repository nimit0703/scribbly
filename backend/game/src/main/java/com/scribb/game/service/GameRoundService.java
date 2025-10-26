package com.scribb.game.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoundService {

    private final RoomManager roomManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final TimerService timerService;
    private final WordBankService wordBankService;
    private final GameProperties gameProperties;

    public boolean shouldStartGame(GameRoom room) {
        return !room.isGameStarted() 
            && room.getPlayers().size() >= gameProperties.getRound().getMinPlayers();
    }

    public void startGame(String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        
        if (room.isGameStarted()) {
            log.warn("Game already started for room: {}", roomId);
            return;
        }
        
        room.setGameStarted(true);
        room.setRoundNumber(1);
        roomManager.updateRoom(room);
        
        log.info("🎮 Starting game for room: {}", roomId);
        startNextRound(roomId);
    }

    public void startRound(String roomId, String drawer, String selectedWord) {
        GameRoom room = roomManager.getRoom(roomId);
        
        if (!drawer.equals(room.getCurrentDrawer())) {
            log.warn("Invalid drawer {} for room {}", drawer, roomId);
            return;
        }
        
        room.setCurrentWord(selectedWord);
        room.setRoundStartTime(System.currentTimeMillis());
        roomManager.updateRoom(room);
        
        log.info("🎯 Round started - Room: {}, Drawer: {}, Word: {}", roomId, drawer, selectedWord);
        
        // Start timer
        timerService.startTimer(roomId);
        
        // Send masked hint to all players
        String maskedWord = "_ ".repeat(selectedWord.length()).trim();
        messagingTemplate.convertAndSend("/topic/hint/" + roomId, maskedWord);
    }

    public void endRound(String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        
        log.info("🏁 Ending round for room: {}", roomId);
        
        // Stop timer
        timerService.stopTimer(roomId);
        
        // Collect current scores
        Map<String, Integer> scores = room.getPlayers().stream()
            .collect(Collectors.toMap(
                Player::getUsername,
                Player::getScore
            ));
        log.info("Round end Scrore::" + scores);
        // Reset for next round
        roomManager.endRound(roomId);
        
        // Broadcast updates
        broadcastRoundEnd(room);
        
        // Check if game is over
        if (room.isGameOver()) {
            endGame(roomId);
        } else {
            // Start next round after delay
            scheduleNextRound(roomId, 5000);
        }
    }

    private void startNextRound(String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        
        String nextDrawer = roomManager.pickNextDrawer(room);
        room.setCurrentDrawer(nextDrawer);
        room.setRoundStartTime(System.currentTimeMillis());
        roomManager.updateRoom(room);
        
        log.info("➡️ Next round - Room: {}, Round: {}, Drawer: {}", 
            roomId, room.getRoundNumber(), nextDrawer);
        
        // Send word options to drawer
        List<String> wordOptions = wordBankService.takeRandom(
            gameProperties.getWord().getOptionsCount()
        );
        messagingTemplate.convertAndSend(
            "/topic/word-options/" + nextDrawer,
            wordOptions
        );
        
        // Notify all players about drawer
        messagingTemplate.convertAndSend(
            "/topic/drawer/" + roomId,
            nextDrawer
        );
    }

    private void broadcastRoundEnd(GameRoom room) {
        String roomId = room.getRoomId();
        
        // Update players list
        messagingTemplate.convertAndSend(
            "/topic/players/" + roomId,
            room.getPlayers()
        );
        
        // Update round number
        messagingTemplate.convertAndSend(
            "/topic/round/" + roomId,
            room.getRoundNumber()
        );
        
        // Clear hint
        messagingTemplate.convertAndSend(
            "/topic/hint/" + roomId,
            ""
        );
        
        // Send round summary
        messagingTemplate.convertAndSend(
            "/topic/round-summary/" + roomId,
            createRoundSummary(room)
        );
    }

    @Async
    protected void scheduleNextRound(String roomId, long delayMillis) {
        try {
            Thread.sleep(delayMillis);
            startNextRound(roomId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error scheduling next round for room: {}", roomId, e);
        }
    }

    private void endGame(String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        
        log.info("🎉 Game ended for room: {}", roomId);
        
        // Send game over message with final scores
        messagingTemplate.convertAndSend(
            "/topic/game-over/" + roomId,
            createGameSummary(room)
        );
        
        messagingTemplate.convertAndSend(
            "/topic/system/" + roomId,
            "🎉 Game Over!"
        );
    }

    private Map<String, Object> createRoundSummary(GameRoom room) {
        return Map.of(
            "roundNumber", room.getRoundNumber(),
            "word", room.getCurrentWord() != null ? room.getCurrentWord() : "",
            "correctGuessers", room.getCorrectGuessers(),
            "players", room.getPlayers()
        );
    }

    private Map<String, Object> createGameSummary(GameRoom room) {
        var sortedPlayers = room.getPlayers().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()))
            .toList();
        
        return Map.of(
            "finalScores", sortedPlayers,
            "winner", sortedPlayers.isEmpty() ? null : sortedPlayers.get(0),
            "totalRounds", room.getRoundNumber()
        );
    }
}