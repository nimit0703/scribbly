package com.scribb.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.scribb.game.model.GameRoom;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;

    private final Map<String, Boolean> activeTimers = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 1000)
    public void broadcastRemainingTime(){
        for( String roomId: activeTimers.keySet()){
            GameRoom room = roomManager.getRoom(roomId);
            if (room != null && room.isTimerActive()){
                int remainingSeconds = room.getRemainingTimeSeconds();
                System.out.println("Timer trigger"+ remainingSeconds);
                messagingTemplate.convertAndSend("/topic/timer/" + roomId, remainingSeconds);

                if (remainingSeconds<=0){
                    stopTimer(roomId);
                    messagingTemplate.convertAndSend("/topic/timer-ended/" + roomId, true);

                }
            }else{
                activeTimers.remove(roomId);
            }
        }

        //send hints at every 10 seconds of game and start of game as well
        for( String roomId: activeTimers.keySet()){
            GameRoom room = roomManager.getRoom(roomId);
            if (room != null && room.isTimerActive()){
                long elapsedMillis = System.currentTimeMillis() - room.getTimerStartTime();
                int elapsedSeconds = (int) (elapsedMillis / 1000);
                if (elapsedSeconds % 10 == 0) {
                    String hint = roomManager.generateHintForRoom(roomId);
                    System.out.println("==============Sending hint: " + hint + " to room: " + roomId);
                    messagingTemplate.convertAndSend("/topic/hint/" + roomId, hint);
                }
            }
        }
    }


    public void startTimer(String roomId){
        GameRoom room = roomManager.getRoom(roomId);
        if(room != null){
            room.startTimer();
            activeTimers.put(roomId,true);

            messagingTemplate.convertAndSend("/topic/timer" + roomId, room.getRemainingTimeSeconds());
        }
    }

    public void stopTimer(String roomId){
        GameRoom room = roomManager.getRoom(roomId);

        if(room != null){
            room.stopTimer();
            activeTimers.remove(roomId);

            messagingTemplate.convertAndSend("/topic/timer/" + roomId, 0);
        }
    }
    public void resetTimer(String roomId) {
        stopTimer(roomId);
    }

    

}
