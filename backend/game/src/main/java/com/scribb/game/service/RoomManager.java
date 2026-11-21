package com.scribb.game.service;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.RoomSettings;
import com.scribb.game.repository.GameRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Owns the set of rooms and which STOMP session plays in which room. No game rules here. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomManager {

    /** No 0/O/1/I so codes are easy to read out loud. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Binding(String code, String playerId) {}

    private final GameRoomRepository repository;
    private final GameProperties props;
    private final Map<String, Binding> sessions = new ConcurrentHashMap<>();

    /** The fullest public room that still has a seat, or a new one. Synchronized so two players don't open two rooms. */
    public synchronized GameRoom findOrCreatePublic() {
        return repository.findAll().stream()
                .filter(r -> !r.isPrivate() && !r.isClosed() && r.getPlayers().size() < r.getSettings().getMaxPlayers())
                .max(Comparator.comparingInt(r -> r.getPlayers().size()))
                .orElseGet(() -> create(false));
    }

    public synchronized GameRoom create(boolean isPrivate) {
        String code;
        do {
            code = randomCode();
        } while (repository.existsById(code));
        RoomSettings settings = new RoomSettings();
        if (!isPrivate) settings.setMaxPlayers(props.getPublicMaxPlayers());
        GameRoom room = new GameRoom(code, isPrivate, settings);
        log.info("Created {} room {}", isPrivate ? "private" : "public", code);
        return repository.save(room);
    }

    public Optional<GameRoom> find(String code) {
        return code == null ? Optional.empty() : repository.findById(code.trim().toUpperCase());
    }

    public void delete(GameRoom room) {
        room.setClosed(true);
        repository.deleteById(room.getCode());
        log.info("Deleted empty room {}", room.getCode());
    }

    public Collection<GameRoom> all() {
        return repository.findAll();
    }

    public void bind(String sessionId, String code, String playerId) {
        sessions.put(sessionId, new Binding(code, playerId));
    }

    public Binding unbind(String sessionId) {
        return sessionId == null ? null : sessions.remove(sessionId);
    }

    public Binding binding(String sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    static String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
