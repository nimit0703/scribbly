package com.scribb.game.service;

import com.scribb.game.model.GameRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameRoundService {

    private final RoomManager roomManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final TimerService timerService;


    public void endRound(String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;

        timerService.resetTimer(roomId);
        roomManager.endRound(roomId);

        messagingTemplate.convertAndSend("/topic/players/" + roomId, room.getPlayers());
        messagingTemplate.convertAndSend("/topic/round/" + roomId, room.getRoundNumber());
        messagingTemplate.convertAndSend("/topic/hint/" + roomId, "");

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
}
