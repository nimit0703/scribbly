package com.scribb.game.service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuessProcessingService {
    
    private final RoomManager roomManager;
    private final GameProperties gameProperties;
    
    public GuessResult processGuess(String roomId, String username, String guess) {
        GameRoom room = roomManager.getRoomOptional(roomId).orElse(null);
        
        if (room == null) {
            return GuessResult.invalid("Room not found");
        }
        
        // Validation
        if (!isValidGuess(room, username)) {
            return GuessResult.invalid("Invalid guess");
        }
        
        // Check correctness
        if (!isCorrectGuess(room, guess)) {
            return GuessResult.incorrect();
        }
        
        // Already guessed
        if (room.getCorrectGuessers().contains(username)) {
            return GuessResult.alreadyGuessed();
        }
        
        // Award points
        long timeTaken = System.currentTimeMillis() - room.getRoundStartTime();
        int guesserPoints = calculateGuesserScore(timeTaken);
        
        room.getPlayer(username).ifPresent(player -> {
            player.setHasGuessedCorrectly(true);
            player.setScore(player.getScore() + guesserPoints);
        });
        
        room.getCorrectGuessers().add(username);
        
        // Award drawer bonus
        room.getPlayer(room.getCurrentDrawer()).ifPresent(drawer -> {
            int drawerBonus = gameProperties.getScoring().getDrawerBonusPerGuesser();
            drawer.setScore(drawer.getScore() + drawerBonus);
        });
        
        roomManager.updateRoom(room);
        
        log.info("✅ Correct guess - Room: {}, Player: {}, Points: {}", roomId, username, guesserPoints);
        
        // Check if round should end
        if (allPlayersGuessed(room)) {
            return GuessResult.correctAndRoundComplete(guesserPoints);
        }
        
        return GuessResult.correct(guesserPoints);
    }
    
    private boolean isValidGuess(GameRoom room, String username) {
        return room.getCurrentWord() != null 
            && !username.equals(room.getCurrentDrawer());
    }
    
    private boolean isCorrectGuess(GameRoom room, String guess) {
        return guess.trim().equalsIgnoreCase(room.getCurrentWord());
    }
    
    private boolean allPlayersGuessed(GameRoom room) {
        return room.getPlayers().stream()
            .filter(p -> !p.getUsername().equals(room.getCurrentDrawer()))
            .allMatch(Player::isHasGuessedCorrectly);
    }
    
    private int calculateGuesserScore(long timeTakenMillis) {
        long seconds = timeTakenMillis / 1000;
        int maxBonus = gameProperties.getScoring().getMaxBonus();
        int minBonus = gameProperties.getScoring().getMinBonus();
        return (int) Math.max(minBonus, maxBonus - seconds);
    }
}