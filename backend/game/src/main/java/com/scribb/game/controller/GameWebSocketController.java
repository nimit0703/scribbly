package com.scribb.game.controller;

import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.scribb.game.model.*;
import com.scribb.game.service.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;
    private final GameRoundService gameRoundService;
    private final GuessProcessingService guessProcessingService;
    private final TimerService timerService;

    @MessageMapping("/join")
    public void joinRoom(@Payload JoinRequest request) {
        try {
            log.info("Player {} joining room {}", request.getUsername(), request.getRoomId());
            
            GameRoom room = roomManager.createOrJoinRoom(request.getRoomId(), request.getUsername());

            // Notify all clients about players
            messagingTemplate.convertAndSend("/topic/players/" + request.getRoomId(), room.getPlayers());

            // Auto-start when minimum players are in room
            if (gameRoundService.shouldStartGame(room)) {
                gameRoundService.startGame(request.getRoomId());
            }
        } catch (Exception e) {
            log.error("Error joining room", e);
            sendError(request.getRoomId(), "Failed to join room: " + e.getMessage());
        }
    }

    @MessageMapping("/draw")
    public void receiveDraw(@Payload DrawMessage message) {
        messagingTemplate.convertAndSend("/topic/draw/" + message.getRoomId(), message);
    }

    @MessageMapping("/draw-start")
    public void drawStart(@Payload DrawStartMessage message) {
        messagingTemplate.convertAndSend("/topic/draw-start/" + message.getRoomId(), message);
    }

    @MessageMapping("/draw-end")
    public void drawEnd(@Payload DrawEndMessage message) {
        messagingTemplate.convertAndSend("/topic/draw-end/" + message.getRoomId(), message);
    }

    @MessageMapping("/clear")
    public void clearCanvas(@Payload ClearMessage message) {
        messagingTemplate.convertAndSend("/topic/clear/" + message.getRoomId(), message);
    }

    @MessageMapping("/chat")
    public void receiveChat(@Payload ChatMessage message) {
        try {
            GuessResult result = guessProcessingService.processGuess(
                message.getRoomId(), 
                message.getUsername(), 
                message.getContent()
            );
            
            log.debug("Guess result for {}: {}", message.getUsername(), result.getMessage());
            
            if (result.isCorrect()) {
                // Send system message about correct guess
                messagingTemplate.convertAndSend(
                    "/topic/system/" + message.getRoomId(),
                    message.getUsername() + " guessed the word! (+" + result.getPointsAwarded() + " points)"
                );

                // Update player list (scores changed)
                GameRoom room = roomManager.getRoom(message.getRoomId());
                messagingTemplate.convertAndSend("/topic/players/" + message.getRoomId(), room.getPlayers());

                // Check if all players guessed
                if (result.isRoundComplete()) {
                    gameRoundService.endRound(message.getRoomId());
                }
            } else {
                // Regular chat message
                messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
            }
        } catch (Exception e) {
            log.error("Error processing chat", e);
        }
    }

    @MessageMapping("/word-select")
    public void wordSelect(@Payload WordSelection selection) {
        try {
            log.info("Word selected for room {}: {}", selection.getRoomId(), selection.getWord());
            gameRoundService.startRound(
                selection.getRoomId(), 
                selection.getDrawer(), 
                selection.getWord()
            );
        } catch (Exception e) {
            log.error("Error selecting word", e);
            sendError(selection.getRoomId(), "Failed to start round: " + e.getMessage());
        }
    }

    @MessageMapping("/start-round")
    public void startRound(@Payload StartRoundRequest req) {
        try {
            gameRoundService.startGame(req.getRoomId());
        } catch (Exception e) {
            log.error("Error starting round", e);
            sendError(req.getRoomId(), "Failed to start round: " + e.getMessage());
        }
    }

    @MessageMapping("/end-round")
    public void endRound(@Payload RoundEndRequest req) {
        try {
            gameRoundService.endRound(req.getRoomId());
        } catch (Exception e) {
            log.error("Error ending round", e);
            sendError(req.getRoomId(), "Failed to end round: " + e.getMessage());
        }
    }

    @MessageMapping("/timer-ended")
    public void handleTimerEnded(@Payload TimerEndedMessage message) {
        try {
            log.info("Timer ended for room: {}", message.getRoomId());
            gameRoundService.endRound(message.getRoomId());
        } catch (Exception e) {
            log.error("Error handling timer end", e);
        }
    }

    @MessageMapping("/hint")
    public void sendHint(@Payload ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/hint/" + message.getRoomId(), message.getContent());
    }

    @MessageMapping("/leave")
    public void leaveRoom(@Payload JoinRequest request) {
        try {
            log.info("Player {} leaving room {}", request.getUsername(), request.getRoomId());
            roomManager.removePlayer(request.getRoomId(), request.getUsername());
            
            GameRoom room = roomManager.getRoomOptional(request.getRoomId()).orElse(null);
            if (room != null) {
                messagingTemplate.convertAndSend("/topic/players/" + request.getRoomId(), room.getPlayers());
            }
        } catch (Exception e) {
            log.error("Error leaving room", e);
        }
    }

    private void sendError(String roomId, String error) {
        messagingTemplate.convertAndSend(
            "/topic/error/" + roomId,
            java.util.Map.of("error", error, "timestamp", System.currentTimeMillis())
        );
    }
}