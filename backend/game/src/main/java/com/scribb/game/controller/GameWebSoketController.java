package com.scribb.game.controller;

import com.scribb.game.model.*;
import com.scribb.game.service.GameRoundService;
import com.scribb.game.service.RoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
@Controller
public class GameWebSoketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;
    private final GameRoundService gameRoundService;

    public GameWebSoketController(SimpMessagingTemplate messagingTemplate, RoomManager roomManager, GameRoundService gameRoundService) {
        this.messagingTemplate = messagingTemplate;
        this.roomManager = roomManager;
        this.gameRoundService = gameRoundService;
    }

    @MessageMapping("/join")
    public void joinRoom(@Payload JoinRequest request) {
        GameRoom room = roomManager.createOrJoinRoom(request.getRoomId(), request.getUsername());

        // Notify all clients about players
        messagingTemplate.convertAndSend("/topic/players/" + request.getRoomId(), room.getPlayers());

        // ✅ Auto-start when minimum 2 players are in room (or use 1 for testing)
        if (room.getPlayers().size() == 2 && !room.isGameStarted()) {
            room.setGameStarted(true); // flag to prevent retrigger

            String nextDrawer = roomManager.pickNextDrawer(room);
            room.setCurrentDrawer(nextDrawer);
            room.setRoundStartTime(System.currentTimeMillis());

            // Send word choices to drawer
            List<String> words = roomManager.getRandomWords();
            messagingTemplate.convertAndSend("/topic/word-options/" + nextDrawer, words);

            // Notify all about who is drawing
            messagingTemplate.convertAndSend("/topic/drawer/" + request.getRoomId(), nextDrawer);
        }
    }

    @MessageMapping("/draw")
    public void receiveDraw(@Payload DrawMessage message) {
        messagingTemplate.convertAndSend("/topic/draw/" + message.getRoomId(), message);
    }

    @MessageMapping("/chat")
    public void receiveChat(@Payload ChatMessage message) {
        boolean correct = roomManager.processGuess(message.getRoomId(), message.getUsername(), message.getContent());
        System.out.println(correct);
        GameRoom room = roomManager.getRoom(message.getRoomId());

        if (correct) {
            messagingTemplate.convertAndSend("/topic/system/" + message.getRoomId(),
                    message.getUsername() + " guessed the word!");

            // update player list (scores changed)
            messagingTemplate.convertAndSend("/topic/players/" + message.getRoomId(), room.getPlayers());
            boolean allGuessed = room.getPlayers().stream()
                    .filter(p -> !p.getUsername().equals(room.getCurrentDrawer()))
                    .allMatch(p -> p.isHasGuessedCorrectly());

            if (allGuessed) {
                endRoundInternal(message.getRoomId());
            }
        } else {
            // regular chat
            messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
        }
    }

    private void endRoundInternal(String roomId) {
        roomManager.endRound(roomId);

        GameRoom room = roomManager.getRoom(roomId);

        // 📢 Notify clients about updated players and new round
        messagingTemplate.convertAndSend("/topic/players/" + roomId, room.getPlayers());
        messagingTemplate.convertAndSend("/topic/round/" + roomId, room.getRoundNumber());

        // 🧼 Clear canvas + hint (frontend can reset when round changes)
        messagingTemplate.convertAndSend("/topic/hint/" + roomId, "");

        // ✅ Auto-start next round if not game over
        if (!room.isGameOver()) {
            String nextDrawer = roomManager.pickNextDrawer(room);
            room.setCurrentDrawer(nextDrawer);
            room.setRoundStartTime(System.currentTimeMillis());

            List<String> words = roomManager.getRandomWords();
            messagingTemplate.convertAndSend("/topic/word-options/" + nextDrawer, words);
            messagingTemplate.convertAndSend("/topic/drawer/" + roomId, nextDrawer);
        } else {
            messagingTemplate.convertAndSend("/topic/system/" + roomId, "🎉 Game Over!");
        }
    }

    // 🧠 4. Drawer selects a word
    @MessageMapping("/word-select")
    public void wordSelect(@Payload WordSelection selection) {
        roomManager.setWord(selection.getRoomId(), selection.getDrawer(), selection.getWord());

        // Send word hint to other players (e.g., "_ _ _")
        String masked = selection.getWord().replaceAll(".", "_ ");
        messagingTemplate.convertAndSend("/topic/hint/" + selection.getRoomId(), masked);
    }

    // 🕹️ 5. Start round (send word choices to drawer only)
//    @MessageMapping("/start-round")
//    public void startRound(@Payload StartRoundRequest req) {
//        List<String> words = roomManager.getRandomWords();
//        messagingTemplate.convertAndSend("/topic/word-options/" + req.getDrawer(), words);
//    }
    @MessageMapping("/start-round")
    public void startRound(@Payload StartRoundRequest req) {
        GameRoom room = roomManager.getRoom(req.getRoomId());
        if (room == null) return;

        // 🔁 Pick the next drawer in rotation
        String nextDrawer = roomManager.pickNextDrawer(room);
        room.setCurrentDrawer(nextDrawer);
        room.setRoundStartTime(System.currentTimeMillis());

        // 🎯 Send word choices to the drawer
        List<String> words = roomManager.getRandomWords();
        messagingTemplate.convertAndSend("/topic/word-options/" + nextDrawer, words);

        // 📢 Notify all players who is the drawer
        messagingTemplate.convertAndSend("/topic/drawer/" + req.getRoomId(), nextDrawer);
    }

    // ⏹️ 6. End current round
    @MessageMapping("/end-round")
    public void endRound(@Payload RoundEndRequest req) {
        gameRoundService.endRound(req.getRoomId());
    }
}
