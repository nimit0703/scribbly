package com.scribb.game.service;

import java.util.Map;
import java.util.concurrent.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.GameRoom;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TimerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomManager roomManager;
    private final HintService hintService;
    private final GameProperties gameProperties;
    
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> activeTimers;
    private final Map<String, ScheduledFuture<?>> hintSchedulers;

    public TimerService(
            SimpMessagingTemplate messagingTemplate,
            RoomManager roomManager,
            HintService hintService,
            GameProperties gameProperties) {
        this.messagingTemplate = messagingTemplate;
        this.roomManager = roomManager;
        this.hintService = hintService;
        this.gameProperties = gameProperties;
        this.scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("timer-pool-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        );
        this.activeTimers = new ConcurrentHashMap<>();
        this.hintSchedulers = new ConcurrentHashMap<>();
        
        log.info("TimerService initialized with thread pool");
    }

    public void startTimer(String roomId) {
        // Cancel any existing timers for this room
        stopTimer(roomId);
        
        GameRoom room = roomManager.getRoomOptional(roomId).orElse(null);
        if (room == null) {
            log.warn("Cannot start timer for non-existent room: {}", roomId);
            return;
        }
        
        room.startTimer();
        roomManager.updateRoom(room);
        
        // Schedule timer broadcasts every second
        ScheduledFuture<?> timerFuture = scheduler.scheduleAtFixedRate(
            () -> broadcastTimer(roomId),
            0,
            1000,
            TimeUnit.MILLISECONDS
        );
        
        activeTimers.put(roomId, timerFuture);
        
        // Schedule hints
        scheduleHints(roomId);
        
        log.info("⏰ Timer started for room: {}", roomId);
    }

    public void stopTimer(String roomId) {
        // Cancel timer
        ScheduledFuture<?> timerFuture = activeTimers.remove(roomId);
        if (timerFuture != null) {
            timerFuture.cancel(false);
        }
        
        // Cancel hint scheduler
        ScheduledFuture<?> hintFuture = hintSchedulers.remove(roomId);
        if (hintFuture != null) {
            hintFuture.cancel(false);
        }
        
        roomManager.getRoomOptional(roomId).ifPresent(room -> {
            room.stopTimer();
            roomManager.updateRoom(room);
        });
        
        log.info("⏸️ Timer stopped for room: {}", roomId);
    }

    public void resetTimer(String roomId) {
        stopTimer(roomId);
    }

    private void broadcastTimer(String roomId) {
        try {
            GameRoom room = roomManager.getRoomOptional(roomId).orElse(null);
            if (room == null || !room.isTimerActive()) {
                stopTimer(roomId);
                return;
            }
            
            int remainingSeconds = room.getRemainingTimeSeconds();
            messagingTemplate.convertAndSend("/topic/timer/" + roomId, remainingSeconds);
            
            if (remainingSeconds <= 0) {
                stopTimer(roomId);
                messagingTemplate.convertAndSend("/topic/timer-ended/" + roomId, true);
            }
        } catch (Exception e) {
            log.error("Error broadcasting timer for room: {}", roomId, e);
            stopTimer(roomId);
        }
    }

    private void scheduleHints(String roomId) {
        int hintInterval = gameProperties.getTimer().getHintIntervalSeconds();
        
        // Schedule hints every N seconds
        ScheduledFuture<?> hintFuture = scheduler.scheduleAtFixedRate(
            () -> broadcastHint(roomId),
            hintInterval,
            hintInterval,
            TimeUnit.SECONDS
        );
        
        hintSchedulers.put(roomId, hintFuture);
    }

    private void broadcastHint(String roomId) {
        try {
            GameRoom room = roomManager.getRoomOptional(roomId).orElse(null);
            if (room == null || !room.isTimerActive()) {
                return;
            }
            
            String hint = hintService.generateHint(roomId);
            messagingTemplate.convertAndSend("/topic/hint/" + roomId, hint);
            log.debug("💡 Hint sent for room {}: {}", roomId, hint);
        } catch (Exception e) {
            log.error("Error broadcasting hint for room: {}", roomId, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TimerService...");
        
        // Cancel all active timers
        activeTimers.values().forEach(future -> future.cancel(false));
        hintSchedulers.values().forEach(future -> future.cancel(false));
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("TimerService shutdown complete");
    }
}