package com.scribb.game.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class WebSocketConfigTest {

    static boolean allowed(StompCommand cmd, String dest) {
        StompHeaderAccessor h = StompHeaderAccessor.create(cmd);
        h.setDestination(dest);
        return WebSocketConfig.isAllowed(h);
    }

    @Test
    void clientsCanOnlySendToTheGame() {
        assertThat(allowed(StompCommand.SEND, "/app/chat")).isTrue();
        assertThat(allowed(StompCommand.SEND, "/topic/room/ABC123")).isFalse(); // would forge strokes/chat
        assertThat(allowed(StompCommand.SEND, "/queue/me")).isFalse();
    }

    @Test
    void clientsCanOnlySubscribeToRoomsAndTheirOwnQueue() {
        assertThat(allowed(StompCommand.SUBSCRIBE, "/user/queue/me")).isTrue();
        assertThat(allowed(StompCommand.SUBSCRIBE, "/topic/room/ABC123")).isTrue();
        assertThat(allowed(StompCommand.SUBSCRIBE, "/queue/me-userabc")).isFalse(); // someone else's private queue
        assertThat(allowed(StompCommand.SUBSCRIBE, "/topic/room/ABC123/../x")).isFalse();
        assertThat(allowed(StompCommand.SUBSCRIBE, "/topic/**")).isFalse();
    }
}
