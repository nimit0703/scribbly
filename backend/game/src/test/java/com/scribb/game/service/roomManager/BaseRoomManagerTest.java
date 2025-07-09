package com.scribb.game.service.roomManager;

import com.scribb.game.model.GameRoom;
import com.scribb.game.service.GameRoundService;
import com.scribb.game.service.RoomManager;
import com.scribb.game.service.TimerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
abstract class BaseRoomManagerTest {

    @Mock protected GameRoundService gameRoundService;

    @Mock protected TimerService timerService;

    @InjectMocks protected RoomManager roomManager;

    protected Map<String, GameRoom> rooms;

    @BeforeEach
    void setUp(){
        rooms = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(roomManager, "rooms", rooms);
    }
}
