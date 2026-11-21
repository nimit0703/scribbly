package com.scribb.game.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.scribb.game.model.ChatLine;
import com.scribb.game.model.DrawOp;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Phase;
import com.scribb.game.model.Player;
import com.scribb.game.model.RoomView;

import lombok.RequiredArgsConstructor;

/**
 * All outbound traffic. Two channels:
 * /topic/room/{code} for everyone in a room, and /user/queue/me for one player (word, word choices, errors).
 * Every payload is an envelope {"type": ..., ...}.
 */
@Service
@RequiredArgsConstructor
public class GameBroadcaster {

    private final SimpMessagingTemplate template;

    public void state(GameRoom room, long now) {
        toRoom(room, Map.of("type", "state", "state", view(room, now)));
    }

    public void chat(GameRoom room, ChatLine line) {
        toRoom(room, Map.of("type", "chat", "line", line));
    }

    public void ops(GameRoom room, List<DrawOp> ops) {
        toRoom(room, Map.of("type", "draw", "ops", ops));
    }

    /** Replace the whole canvas (after clear, undo, or a new turn). */
    public void sync(GameRoom room) {
        toRoom(room, Map.of("type", "sync", "ops", List.copyOf(room.getOps())));
    }

    public void chatTo(Player p, ChatLine line) {
        toSession(p.getSessionId(), Map.of("type", "chat", "line", line));
    }

    public void toPlayer(Player p, Map<String, Object> payload) {
        toSession(p.getSessionId(), payload);
    }

    public void error(String sessionId, String message) {
        toSession(sessionId, Map.of("type", "error", "message", message));
    }

    /** Everything a client needs to render the room from scratch. */
    public void welcome(GameRoom room, Player p, long now) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "welcome");
        payload.put("you", p.getId());
        payload.put("state", view(room, now));
        payload.put("ops", List.copyOf(room.getOps()));
        boolean drawer = p.getId().equals(room.getDrawerId());
        if (room.getWord() != null && (drawer || p.isGuessed())) payload.put("word", room.getWord());
        if (drawer && room.getPhase() == Phase.CHOOSING) payload.put("words", room.getWordChoices());
        toSession(p.getSessionId(), payload);
    }

    public static RoomView view(GameRoom room, long now) {
        Phase phase = room.getPhase();
        String hint = phase == Phase.DRAWING && room.getWord() != null ? Hints.mask(room.getWord(), room.getRevealed()) : null;
        int timeLeft = phase == Phase.LOBBY ? 0 : (int) Math.max(0, Math.ceil((room.getDeadline() - now) / 1000.0));
        return new RoomView(room.getCode(), room.isPrivate(), room.getOwnerId(), phase, room.getSettings(),
                room.getRound(), room.getDrawerId(), hint, timeLeft, List.copyOf(room.getPlayers()), room.getTurnResult());
    }

    private void toRoom(GameRoom room, Object payload) {
        template.convertAndSend("/topic/room/" + room.getCode(), payload);
    }

    /** Address one STOMP session directly; works without a logged-in Principal. */
    private void toSession(String sessionId, Object payload) {
        if (sessionId == null) return;
        SimpMessageHeaderAccessor h = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        h.setSessionId(sessionId);
        h.setLeaveMutable(true);
        template.convertAndSendToUser(sessionId, "/queue/me", payload, h.getMessageHeaders());
    }
}
