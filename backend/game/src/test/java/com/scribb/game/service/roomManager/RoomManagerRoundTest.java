// package com.scribb.game.service.roomManager;

// import com.scribb.game.model.GameRoom;
// import com.scribb.game.model.Player;
// import org.junit.jupiter.api.Test;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// class RoomManagerRoundTest extends BaseRoomManagerTest {

//     @Test
//     void testEndRound_ValidRoom() {
//         String roomId = "room1";
//         GameRoom room = mock(GameRoom.class);
//         rooms.put(roomId, room);

//         roomManager.endRound(roomId);

//         verify(room).resetForNextRound();
//     }

//     @Test
//     void testEndRound_NonExistentRoom() {
//         assertDoesNotThrow(() -> roomManager.endRound("nonexistent"));
//     }

//     @Test
//     void testIsRoundOver_NonExistentRoom() {
//         assertTrue(roomManager.isRoundOver("nonexistent"));
//     }

//     @Test
//     void testIsRoundOver_TimeExpired() {
//         GameRoom room = new GameRoom("room1");
//         room.setRoundStartTime(System.currentTimeMillis() - 70000);
//         room.setCurrentWord("apple");
//         room.getPlayers().add(new Player("player1", 0, false));
//         rooms.put("room1", room);

//         assertTrue(roomManager.isRoundOver("room1"));
//     }

//     @Test
//     void testIsRoundOver_AllPlayersGuessedCorrectly() {
//         GameRoom room = new GameRoom("room1");
//         room.setRoundStartTime(System.currentTimeMillis());
//         room.setCurrentWord("apple");
//         room.setCurrentDrawer("drawer");
//         room.getPlayers().add(new Player("drawer", 0, false));
//         room.getPlayers().add(new Player("p1", 0, true));
//         room.getPlayers().add(new Player("p2", 0, true));
//         rooms.put("room1", room);

//         assertTrue(roomManager.isRoundOver("room1"));
//     }

//     @Test
//     void testIsRoundOver_NotAllPlayersGuessedCorrectly() {
//         GameRoom room = new GameRoom("room1");
//         room.setRoundStartTime(System.currentTimeMillis());
//         room.setCurrentWord("apple");
//         room.setCurrentDrawer("drawer");
//         room.getPlayers().add(new Player("drawer", 0, false));
//         room.getPlayers().add(new Player("p1", 0, true));
//         room.getPlayers().add(new Player("p2", 0, false));
//         rooms.put("room1", room);

//         assertFalse(roomManager.isRoundOver("room1"));
//     }

//     @Test
//     void testPickNextDrawer_ValidRoom() {
//         GameRoom room = new GameRoom("room1");
//         room.getPlayers().add(new Player("player1", 0, false));
//         room.getPlayers().add(new Player("player2", 0, false));
//         room.getPlayers().add(new Player("player3", 0, false));
//         room.setLastDrawerIndex(-1);

//         assertEquals("player1", roomManager.pickNextDrawer(room));
//         assertEquals("player2", roomManager.pickNextDrawer(room));
//         assertEquals("player3", roomManager.pickNextDrawer(room));
//         assertEquals("player1", roomManager.pickNextDrawer(room));
//         assertEquals(0, room.getLastDrawerIndex());
//     }

//     @Test
//     void testPickNextDrawer_EmptyRoom() {
//         GameRoom room = new GameRoom("room1");
//         assertNull(roomManager.pickNextDrawer(room));
//     }

//     @Test
//     void testPickNextDrawer_SinglePlayer() {
//         GameRoom room = new GameRoom("room1");
//         room.getPlayers().add(new Player("player1", 0, false));
//         room.setLastDrawerIndex(-1);

//         assertEquals("player1", roomManager.pickNextDrawer(room));
//         assertEquals("player1", roomManager.pickNextDrawer(room));
//         assertEquals(0, room.getLastDrawerIndex());
//     }

//     @Test
//     void testInit() {
//         assertDoesNotThrow(() -> roomManager.init());
//     }

//     // Add checkRoundTimeouts tests here from the commented section
// }
