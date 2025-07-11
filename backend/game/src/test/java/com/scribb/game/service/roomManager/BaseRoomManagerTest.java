package com.scribb.game.service.roomManager;

import com.scribb.game.model.GameRoom;
import com.scribb.game.service.GameRoundService;
import com.scribb.game.service.RoomManager;
import com.scribb.game.service.TimerService;
import com.scribb.game.service.WordBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
abstract class BaseRoomManagerTest {

    @Mock protected GameRoundService gameRoundService;

    @Mock protected TimerService timerService;

    @Mock protected WordBankService wordBankService;

    @InjectMocks protected RoomManager roomManager;

    protected Map<String, GameRoom> rooms;

    @BeforeEach
    void setUp(){
        rooms = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(roomManager, "rooms", rooms);

        when(wordBankService.takeRandom(3))
                .thenReturn(List.of("apple", "banana", "cat"));

    }
}
