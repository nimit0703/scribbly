// GameRoomRepository.java (Interface)
package com.scribb.game.repository;

import java.util.Collection;
import java.util.Optional;

import com.scribb.game.model.GameRoom;

public interface GameRoomRepository {
    Optional<GameRoom> findById(String roomId);
    GameRoom save(GameRoom room);
    void deleteById(String roomId);
    Collection<GameRoom> findAll();
    boolean existsById(String roomId);
}
