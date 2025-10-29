// InMemoryGameRoomRepository.java (Implementation)
package com.scribb.game.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.scribb.game.model.GameRoom;

@Repository
public class InMemoryGameRoomRepository implements GameRoomRepository {
    
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    
    @Override
    public Optional<GameRoom> findById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }
    
    @Override
    public GameRoom save(GameRoom room) {
        rooms.put(room.getRoomId(), room);
        room.setLastActivityTime(System.currentTimeMillis());
        return room;
    }
    
    @Override
    public void deleteById(String roomId) {
        rooms.remove(roomId);
    }
    
    @Override
    public Collection<GameRoom> findAll() {
        return new ArrayList<>(rooms.values());
    }
    
    @Override
    public boolean existsById(String roomId) {
        return rooms.containsKey(roomId);
    }
}