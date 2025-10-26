// package com.scribb.game.service.timeService;

// import com.scribb.game.model.GameRoom;
// import com.scribb.game.service.RoomManager;
// import com.scribb.game.service.TimerService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.messaging.simp.SimpMessagingTemplate;

// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.mockito.Mockito.*;

// public class TimeServiceTest {
//     private SimpMessagingTemplate messagingTemplate;
//     private RoomManager roomManager;
//     private TimerService timerService;
//     private Map<String, GameRoom> rooms;

//     @BeforeEach
//     void setUp() {
//         messagingTemplate = mock(SimpMessagingTemplate.class);
//         roomManager = mock(RoomManager.class);
//         timerService = new TimerService(messagingTemplate, roomManager);

//         rooms = new ConcurrentHashMap<>();
//     }

//     @Test
//     void testStartTimer() {
//         GameRoom room = mock(GameRoom.class);
//         when(room.getRemainingTimeSeconds()).thenReturn(60);
//         when(roomManager.getRoom("room1")).thenReturn(room);

//         timerService.startTimer("room1");

//         verify(room).startTimer();
//         verify(messagingTemplate).convertAndSend("/topic/timerroom1", 60);
//     }

//     @Test
//     void testStopTimer() {
//         GameRoom room = mock(GameRoom.class);
//         when(roomManager.getRoom("room1")).thenReturn(room);

//         timerService.stopTimer("room1");

//         verify(room).stopTimer();
//         verify(messagingTemplate).convertAndSend("/topic/timer/room1", 0);
//     }

//     @Test
//     void testResetTimer() {
//         GameRoom room = mock(GameRoom.class);
//         when(roomManager.getRoom("room1")).thenReturn(room);

//         timerService.resetTimer("room1");

//         verify(room).stopTimer();
//         verify(messagingTemplate).convertAndSend("/topic/timer/room1", 0);
//     }

//     @Test
//     void testBroadcastRemainingTime_TimerActiveAndExpires() {
//         GameRoom room = mock(GameRoom.class);
//         when(roomManager.getRoom("room1")).thenReturn(room);
//         when(room.isTimerActive()).thenReturn(true);
//         when(room.getRemainingTimeSeconds()).thenReturn(0);

//         // Start timer manually to populate internal map
//         timerService.startTimer("room1");

//         timerService.broadcastRemainingTime();

//         verify(messagingTemplate, times(2)).convertAndSend("/topic/timer/room1", (Object) 0);
//         verify(messagingTemplate).convertAndSend("/topic/timer-ended/room1", true);
//     }

//     @Test
//     void testBroadcastRemainingTime_TimerActiveStillRunning() {
//         GameRoom room = mock(GameRoom.class);
//         when(roomManager.getRoom("room1")).thenReturn(room);
//         when(room.isTimerActive()).thenReturn(true);
//         when(room.getRemainingTimeSeconds()).thenReturn(45);

//         timerService.startTimer("room1");

//         timerService.broadcastRemainingTime();

//         verify(messagingTemplate).convertAndSend("/topic/timer/room1", 45);
//         verify(messagingTemplate, never()).convertAndSend(contains("timer-ended"), (Object) any());

//     }

//     @Test
//     void testBroadcastRemainingTime_NoRoomFound() {
//         timerService.startTimer("room1");
//         when(roomManager.getRoom("room1")).thenReturn(null);

//         // should not throw
//         assertDoesNotThrow(() -> timerService.broadcastRemainingTime());
//     }
// }
