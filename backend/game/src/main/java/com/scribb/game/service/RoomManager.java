package com.scribb.game.service;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.exception.RoomNotFoundException;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;
import com.scribb.game.repository.GameRoomRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomManager {
    
    private final GameRoomRepository roomRepository;
    private final WordBankService wordBankService;
    private final GameProperties gameProperties;
    
    @PostConstruct
    public void init() {
        log.info("===== RoomManager initialized =====");
    }

    public GameRoom createOrJoinRoom(String roomId, String username) {
        GameRoom room = roomRepository.findById(roomId)
            .orElseGet(() -> {
                log.info("Creating new room: {}", roomId);
                return new GameRoom(roomId);
            });
        
        if (room.getPlayer(username).isEmpty()) {
            room.getPlayers().add(new Player(username, 0, false));
            log.info("Player {} joined room {}", username, roomId);
        }
        
        return roomRepository.save(room);
    }

    public GameRoom getRoom(String roomId) {
        return roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotFoundException(roomId));
    }
    
    public Optional<GameRoom> getRoomOptional(String roomId) {
        return roomRepository.findById(roomId);
    }

    public void removePlayer(String roomId, String username) {
        Optional<GameRoom> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return;
        }
        
        GameRoom room = roomOpt.get();
        room.getPlayers().removeIf(p -> p.getUsername().equals(username));
        
        if (room.getPlayers().isEmpty()) {
            log.info("Room {} is empty, deleting", roomId);
            roomRepository.deleteById(roomId);
        } else {
            roomRepository.save(room);
        }
    }

    public void endRound(String roomId) {
        GameRoom room = getRoom(roomId);
        room.resetForNextRound();
        roomRepository.save(room);
        log.info("Round ended for room: {}", roomId);
    }

    public Collection<GameRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    public boolean isRoundOver(String roomId) {
        Optional<GameRoom> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return true;
        }
        
        GameRoom room = roomOpt.get();
        long now = System.currentTimeMillis();
        long roundDuration = gameProperties.getRound().getDurationSeconds() * 1000L;
        boolean timeExpired = (now - room.getRoundStartTime()) > roundDuration;
        
        boolean allGuessed = room.getPlayers().stream()
            .filter(p -> !p.getUsername().equals(room.getCurrentDrawer()))
            .allMatch(Player::isHasGuessedCorrectly);

        return timeExpired || allGuessed;
    }

    public String pickNextDrawer(GameRoom room) {
        if (room.getPlayers().isEmpty()) {
            throw new IllegalStateException("No players in room");
        }

        int nextIndex = (room.getLastDrawerIndex() + 1) % room.getPlayers().size();
        room.setLastDrawerIndex(nextIndex);

        String drawer = room.getPlayers().get(nextIndex).getUsername();
        log.info("Next drawer selected for room {}: {}", room.getRoomId(), drawer);
        return drawer;
    }

    public void updateRoom(GameRoom room) {
        roomRepository.save(room);
    }
}