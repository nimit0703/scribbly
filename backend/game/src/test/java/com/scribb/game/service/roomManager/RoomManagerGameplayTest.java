// package com.scribb.game.service.roomManager;

// import com.scribb.game.model.GameRoom;
// import org.junit.jupiter.api.Test;


// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// class RoomManagerGameplayTest extends BaseRoomManagerTest {

//     @Test
//     void testSetWord_ValidRoom() {
//         String roomId = "room1";
//         String drawer = "player1";
//         String word = "apple";

//         GameRoom room = new GameRoom(roomId);
//         room.setCurrentDrawer(drawer);
//         rooms.put(roomId, room);

//         roomManager.setWord(roomId, drawer, word);

//         assertEquals(word, room.getCurrentWord());
//         assertEquals(drawer, room.getCurrentDrawer());
//         assertTrue(room.getRoundStartTime() > 0);
//         verify(timerService).startTimer(roomId);
//     }

//     @Test
//     void testSetWord_NonExistentRoom() {
//         roomManager.setWord("nonexistent", "player1", "apple");
//         verify(timerService, never()).startTimer(any());
//     }

//     @Test
//     void testSetWord_RoomWithoutCurrentDrawer() {
//         String roomId = "room1";
//         GameRoom room = new GameRoom(roomId);
//         rooms.put(roomId, room);

//         roomManager.setWord(roomId, "player1", "apple");

//         assertNull(room.getCurrentWord());
//         verify(timerService, never()).startTimer(any());
//     }

//     // Add all processGuess tests here
//     // Add all score calculation tests here
//     // Keep adding similar logic-based tests from original class
// }