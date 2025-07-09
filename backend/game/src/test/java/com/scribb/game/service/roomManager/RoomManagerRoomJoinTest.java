package com.scribb.game.service.roomManager;

import com.scribb.game.model.GameRoom;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerRoomJoinTest extends BaseRoomManagerTest {

    @Test
    void testCreateOrJoinRoom_NewRoom() {
        String roomId = "room1";
        String username = "player1";

        GameRoom result = roomManager.createOrJoinRoom(roomId, username);

        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(1, result.getPlayers().size());
        assertEquals(username, result.getPlayers().get(0).getUsername());
        assertEquals(0, result.getPlayers().get(0).getScore());
        assertFalse(result.getPlayers().get(0).isHasGuessedCorrectly());
    }

    @Test
    void testCreateOrJoinRoom_ExistingRoom() {
        String roomId = "room1";
        String username1 = "player1";
        String username2 = "player2";

        GameRoom room1 = roomManager.createOrJoinRoom(roomId, username1);
        GameRoom room2 = roomManager.createOrJoinRoom(roomId, username2);

        assertSame(room1, room2);
        assertEquals(2, room1.getPlayers().size());
        assertTrue(room1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(username1)));
        assertTrue(room1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(username2)));
    }

    @Test
    void testCreateOrJoinRoom_SamePlayerJoinsAgain() {
        String roomId = "room1";
        String username = "player1";

        GameRoom room1 = roomManager.createOrJoinRoom(roomId, username);
        GameRoom room2 = roomManager.createOrJoinRoom(roomId, username);

        assertSame(room1, room2);
        assertEquals(1, room1.getPlayers().size());
        assertEquals(username, room1.getPlayers().get(0).getUsername());
    }

    @Test
    void testGetRoom_ExistingRoom() {
        String roomId = "room1";
        GameRoom room = new GameRoom(roomId);
        rooms.put(roomId, room);

        GameRoom result = roomManager.getRoom(roomId);

        assertSame(room, result);
    }

    @Test
    void testGetRoom_NonExistentRoom() {
        String roomId = "nonexistent";
        GameRoom result = roomManager.getRoom(roomId);
        assertNull(result);
    }

    @Test
    void testGetRandomWords() {
        List<String> words = roomManager.getRandomWords();

        assertNotNull(words);
        assertEquals(3, words.size());
        List<String> expectedWords = List.of("apple", "banana", "cat", "dog", "house", "mountain");
        for (String word : words) {
            assertTrue(expectedWords.contains(word));
        }
    }
}
