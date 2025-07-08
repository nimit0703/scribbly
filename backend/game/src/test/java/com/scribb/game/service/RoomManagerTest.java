package com.scribb.game.service;

import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomManagerTest {

    @Mock
    private GameRoundService gameRoundService;

    @Mock
    private TimerService timerService;

    @InjectMocks
    private RoomManager roomManager;

    private Map<String, GameRoom> rooms;

    @BeforeEach
    void setUp() {
        rooms = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(roomManager, "rooms", rooms);
    }

    @Test
    void testCreateOrJoinRoom_NewRoom() {
        // Given
        String roomId = "room1";
        String username = "player1";

        // When
        GameRoom result = roomManager.createOrJoinRoom(roomId, username);

        // Then
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(1, result.getPlayers().size());
        assertEquals(username, result.getPlayers().get(0).getUsername());
        assertEquals(0, result.getPlayers().get(0).getScore());
        assertFalse(result.getPlayers().get(0).isHasGuessedCorrectly());
    }

    @Test
    void testCreateOrJoinRoom_ExistingRoom() {
        // Given
        String roomId = "room1";
        String username1 = "player1";
        String username2 = "player2";

        // When
        GameRoom room1 = roomManager.createOrJoinRoom(roomId, username1);
        GameRoom room2 = roomManager.createOrJoinRoom(roomId, username2);

        // Then
        assertSame(room1, room2);
        assertEquals(2, room1.getPlayers().size());
        assertTrue(room1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(username1)));
        assertTrue(room1.getPlayers().stream().anyMatch(p -> p.getUsername().equals(username2)));
    }

    @Test
    void testCreateOrJoinRoom_SamePlayerJoinsAgain() {
        // Given
        String roomId = "room1";
        String username = "player1";

        // When
        GameRoom room1 = roomManager.createOrJoinRoom(roomId, username);
        GameRoom room2 = roomManager.createOrJoinRoom(roomId, username);

        // Then
        assertSame(room1, room2);
        assertEquals(1, room1.getPlayers().size());
        assertEquals(username, room1.getPlayers().get(0).getUsername());
    }

    @Test
    void testGetRandomWords() {
        // When
        List<String> words = roomManager.getRandomWords();

        // Then
        assertNotNull(words);
        assertEquals(3, words.size());
        // Verify all words are from the expected word bank
        List<String> expectedWords = List.of("apple", "banana", "cat", "dog", "house", "mountain");
        for (String word : words) {
            assertTrue(expectedWords.contains(word));
        }
    }

    @Test
    void testSetWord_ValidRoom() {
        // Given
        String roomId = "room1";
        String drawer = "player1";
        String word = "apple";

        GameRoom room = new GameRoom(roomId);
        room.setCurrentDrawer(drawer);
        rooms.put(roomId, room);

        // When
        roomManager.setWord(roomId, drawer, word);

        // Then
        assertEquals(word, room.getCurrentWord());
        assertEquals(drawer, room.getCurrentDrawer());
        assertTrue(room.getRoundStartTime() > 0);
        verify(timerService).startTimer(roomId);
    }

    @Test
    void testSetWord_NonExistentRoom() {
        // Given
        String roomId = "nonexistent";
        String drawer = "player1";
        String word = "apple";

        // When
        roomManager.setWord(roomId, drawer, word);

        // Then
        verify(timerService, never()).startTimer(any());
    }

    @Test
    void testSetWord_RoomWithoutCurrentDrawer() {
        // Given
        String roomId = "room1";
        String drawer = "player1";
        String word = "apple";

        GameRoom room = new GameRoom(roomId);
        // Not setting currentDrawer
        rooms.put(roomId, room);

        // When
        roomManager.setWord(roomId, drawer, word);

        // Then
        assertNull(room.getCurrentWord());
        verify(timerService, never()).startTimer(any());
    }

    @Test
    void testProcessGuess_CorrectGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "apple";
        String guess = "APPLE";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis() - 5000); // 5 seconds ago
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        assertTrue(result);
        assertTrue(room.getCorrectGuessers().contains(guesserUsername));
        Player guesser = room.getPlayer(guesserUsername).orElse(null);
        assertNotNull(guesser);
        assertTrue(guesser.isHasGuessedCorrectly());
        assertTrue(guesser.getScore() > 0);

        Player drawer = room.getPlayer(drawerUsername).orElse(null);
        assertNotNull(drawer);
        assertEquals(10, drawer.getScore()); // drawer bonus
    }

    @Test
    void testProcessGuess_IncorrectGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "apple";
        String guess = "banana";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis());
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        assertFalse(result);
        assertFalse(room.getCorrectGuessers().contains(guesserUsername));
        Player guesser = room.getPlayer(guesserUsername).orElse(null);
        assertNotNull(guesser);
        assertFalse(guesser.isHasGuessedCorrectly());
        assertEquals(0, guesser.getScore());
    }

    @Test
    void testProcessGuess_DrawerGuessing() {
        // Given
        String roomId = "room1";
        String drawerUsername = "player1";
        String word = "apple";
        String guess = "apple";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis());
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, drawerUsername, guess);

        // Then
        assertFalse(result);
    }

    @Test
    void testProcessGuess_NonExistentRoom() {
        // Given
        String roomId = "nonexistent";
        String username = "player1";
        String guess = "apple";

        // When
        boolean result = roomManager.processGuess(roomId, username, guess);

        // Then
        assertNull(result);
    }

    @Test
    void testProcessGuess_NoCurrentWord() {
        // Given
        String roomId = "room1";
        String username = "player1";
        String guess = "apple";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(username, 0, false));
        // Not setting current word
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, username, guess);

        // Then
        assertFalse(result);
    }

    @Test
    void testProcessGuess_DuplicateCorrectGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "apple";
        String guess = "apple";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis());
        room.getCorrectGuessers().add(guesserUsername); // Already guessed correctly
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        assertFalse(result);
    }

    @Test
    void testProcessGuess_CaseInsensitiveGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "Apple";
        String guess = "  aPpLe  "; // different case with spaces

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis());
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        assertTrue(result);
        assertTrue(room.getCorrectGuessers().contains(guesserUsername));
    }

    @Test
    void testEndRound_ValidRoom() {
        // Given
        String roomId = "room1";
        GameRoom room = mock(GameRoom.class);
        rooms.put(roomId, room);

        // When
        roomManager.endRound(roomId);

        // Then
        verify(room).resetForNextRound();
    }

    @Test
    void testEndRound_NonExistentRoom() {
        // Given
        String roomId = "nonexistent";

        // When & Then (should not throw exception)
        assertDoesNotThrow(() -> roomManager.endRound(roomId));
    }

    @Test
    void testGetRoom_ExistingRoom() {
        // Given
        String roomId = "room1";
        GameRoom room = new GameRoom(roomId);
        rooms.put(roomId, room);

        // When
        GameRoom result = roomManager.getRoom(roomId);

        // Then
        assertSame(room, result);
    }

    @Test
    void testGetRoom_NonExistentRoom() {
        // Given
        String roomId = "nonexistent";

        // When
        GameRoom result = roomManager.getRoom(roomId);

        // Then
        assertNull(result);
    }

    @Test
    void testIsRoundOver_NonExistentRoom() {
        // Given
        String roomId = "nonexistent";

        // When
        boolean result = roomManager.isRoundOver(roomId);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRoundOver_TimeExpired() {
        // Given
        String roomId = "room1";
        GameRoom room = new GameRoom(roomId);
        room.setRoundStartTime(System.currentTimeMillis() - 70_000); // 70 seconds ago
        room.setCurrentWord("apple");
        room.getPlayers().add(new Player("player1", 0, false));
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.isRoundOver(roomId);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRoundOver_AllPlayersGuessedCorrectly() {
        // Given
        String roomId = "room1";
        String drawerUsername = "drawer";
        GameRoom room = new GameRoom(roomId);
        room.setRoundStartTime(System.currentTimeMillis());
        room.setCurrentWord("apple");
        room.setCurrentDrawer(drawerUsername);

        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.getPlayers().add(new Player("player1", 0, true));
        room.getPlayers().add(new Player("player2", 0, true));
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.isRoundOver(roomId);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsRoundOver_NotAllPlayersGuessedCorrectly() {
        // Given
        String roomId = "room1";
        String drawerUsername = "drawer";
        GameRoom room = new GameRoom(roomId);
        room.setRoundStartTime(System.currentTimeMillis());
        room.setCurrentWord("apple");
        room.setCurrentDrawer(drawerUsername);

        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.getPlayers().add(new Player("player1", 0, true));
        room.getPlayers().add(new Player("player2", 0, false)); // Not guessed correctly
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.isRoundOver(roomId);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsRoundOver_TimeNotExpiredAndNotAllGuessed() {
        // Given
        String roomId = "room1";
        String drawerUsername = "drawer";
        GameRoom room = new GameRoom(roomId);
        room.setRoundStartTime(System.currentTimeMillis() - 30_000); // 30 seconds ago
        room.setCurrentWord("apple");
        room.setCurrentDrawer(drawerUsername);

        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.getPlayers().add(new Player("player1", 0, false));
        rooms.put(roomId, room);

        // When
        boolean result = roomManager.isRoundOver(roomId);

        // Then
        assertFalse(result);
    }

    @Test
    void testPickNextDrawer_ValidRoom() {
        // Given
        GameRoom room = new GameRoom("room1");
        room.getPlayers().add(new Player("player1", 0, false));
        room.getPlayers().add(new Player("player2", 0, false));
        room.getPlayers().add(new Player("player3", 0, false));
        room.setLastDrawerIndex(-1);

        // When
        String result1 = roomManager.pickNextDrawer(room);
        String result2 = roomManager.pickNextDrawer(room);
        String result3 = roomManager.pickNextDrawer(room);
        String result4 = roomManager.pickNextDrawer(room); // Should cycle back

        // Then
        assertEquals("player1", result1);
        assertEquals("player2", result2);
        assertEquals("player3", result3);
        assertEquals("player1", result4); // Cycling back
        assertEquals(0, room.getLastDrawerIndex()); // Should be back to index 0
    }

    @Test
    void testPickNextDrawer_EmptyRoom() {
        // Given
        GameRoom room = new GameRoom("room1");
        // No players added

        // When
        String result = roomManager.pickNextDrawer(room);

        // Then
        assertNull(result);
    }

    @Test
    void testPickNextDrawer_SinglePlayer() {
        // Given
        GameRoom room = new GameRoom("room1");
        room.getPlayers().add(new Player("player1", 0, false));
        room.setLastDrawerIndex(-1);

        // When
        String result1 = roomManager.pickNextDrawer(room);
        String result2 = roomManager.pickNextDrawer(room);

        // Then
        assertEquals("player1", result1);
        assertEquals("player1", result2); // Same player again
        assertEquals(0, room.getLastDrawerIndex());
    }

//    @Test
//    void testCheckRoundTimeouts_TimeoutOccurs() {
//        // Given
//        String roomId = "room1";
//        GameRoom room = new GameRoom(roomId);
//        room.setCurrentWord("apple");
//        room.setRoundStartTime(System.currentTimeMillis() - 70_000); // 70 seconds ago
//        room.setGameOver(false);
//        rooms.put(roomId, room);
//
//        // When
//        roomManager.checkRoundTimeouts();
//
//        // Then
//        verify(gameRoundService).endRound(roomId);
//    }
//
//    @Test
//    void testCheckRoundTimeouts_NoTimeout() {
//        // Given
//        String roomId = "room1";
//        GameRoom room = new GameRoom(roomId);
//        room.setCurrentWord("apple");
//        room.setRoundStartTime(System.currentTimeMillis() - 30_000); // 30 seconds ago
//        room.setGameOver(false);
//        rooms.put(roomId, room);
//
//        // When
//        roomManager.checkRoundTimeouts();
//
//        // Then
//        verify(gameRoundService, never()).endRound(any());
//    }
//
//    @Test
//    void testCheckRoundTimeouts_GameOver() {
//        // Given
//        String roomId = "room1";
//        GameRoom room = new GameRoom(roomId);
//        room.setCurrentWord("apple");
//        room.setRoundStartTime(System.currentTimeMillis() - 70_000); // 70 seconds ago
//        room.setGameOver(true); // Game is over
//        rooms.put(roomId, room);
//
//        // When
//        roomManager.checkRoundTimeouts();
//
//        // Then
//        verify(gameRoundService, never()).endRound(any());
//    }
//
//    @Test
//    void testCheckRoundTimeouts_NoCurrentWord() {
//        // Given
//        String roomId = "room1";
//        GameRoom room = new GameRoom(roomId);
//        // No current word set
//        room.setRoundStartTime(System.currentTimeMillis() - 70_000);
//        room.setGameOver(false);
//        rooms.put(roomId, room);
//
//        // When
//        roomManager.checkRoundTimeouts();
//
//        // Then
//        verify(gameRoundService, never()).endRound(any());
//    }

    @Test
    void testInit() {
        // This test just ensures the init method can be called without errors
        // When
        assertDoesNotThrow(() -> roomManager.init());
    }

    @Test
    void testScoreCalculation_QuickGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "apple";
        String guess = "apple";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis() - 1000); // 1 second ago
        rooms.put(roomId, room);

        // When
        roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        Player guesser = room.getPlayer(guesserUsername).orElse(null);
        assertNotNull(guesser);
        assertTrue(guesser.getScore() >= 90); // Should get high bonus for quick guess
    }

    @Test
    void testScoreCalculation_SlowGuess() {
        // Given
        String roomId = "room1";
        String guesserUsername = "player1";
        String drawerUsername = "player2";
        String word = "apple";
        String guess = "apple";

        GameRoom room = new GameRoom(roomId);
        room.getPlayers().add(new Player(guesserUsername, 0, false));
        room.getPlayers().add(new Player(drawerUsername, 0, false));
        room.setCurrentWord(word);
        room.setCurrentDrawer(drawerUsername);
        room.setRoundStartTime(System.currentTimeMillis() - 95_000); // 95 seconds ago
        rooms.put(roomId, room);

        // When
        roomManager.processGuess(roomId, guesserUsername, guess);

        // Then
        Player guesser = room.getPlayer(guesserUsername).orElse(null);
        assertNotNull(guesser);
        assertEquals(10, guesser.getScore()); // Should get minimum bonus
    }
}