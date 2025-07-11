package com.scribb.game.service.gameRound;

import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;
import com.scribb.game.service.GameRoundService;
import com.scribb.game.service.RoomManager;
import com.scribb.game.service.TimerService;
import com.scribb.game.service.WordBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameRoundServiceTest {

    @Mock private RoomManager roomManager;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private TimerService timerService;
    @Mock private WordBankService wordBankService;
    @InjectMocks private GameRoundService gameRoundService;

    @BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testEndRound_RoomIsNull(){
        when(roomManager.getRoom("room1")).thenReturn(null);

        assertDoesNotThrow(() -> gameRoundService.endRound("room1"));

        verifyNoInteractions(timerService);
        verifyNoMoreInteractions(messagingTemplate);
    }

}
