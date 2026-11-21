package com.scribb.game.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.Avatar;
import com.scribb.game.model.ChatLine;
import com.scribb.game.model.DrawOp;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Phase;
import com.scribb.game.model.Player;
import com.scribb.game.model.Requests;
import com.scribb.game.model.RoomSettings;
import com.scribb.game.model.TurnResult;
import com.scribb.game.service.RoomManager.Binding;

import lombok.extern.slf4j.Slf4j;

/**
 * The game rules. One state machine per room:
 *
 *   LOBBY -> CHOOSING -> DRAWING -> TURN_END -> (next turn | GAME_OVER) -> LOBBY
 *
 * Every public method resolves the caller from its STOMP session, then mutates the room while holding
 * synchronized(room). Time-based transitions happen in tick(), driven by a 250 ms scheduler.
 */
@Slf4j
@Service
public class GameEngine {

    static final int MAX_CHAT = 100;
    static final int MAX_OPS = 6000;

    private final RoomManager rooms;
    private final GameBroadcaster out;
    private final WordBankService words;
    private final GameProperties props;
    private final Clock clock;
    private final Random random = new Random();

    public GameEngine(RoomManager rooms, GameBroadcaster out, WordBankService words, GameProperties props, Clock clock) {
        this.rooms = rooms;
        this.out = out;
        this.words = words;
        this.props = props;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ joining and leaving

    // A session plays in one room at a time, so each entry point leaves first (before picking a room,
    // otherwise a lone player could pick the room that their own leave is about to delete).

    public void quickPlay(String sessionId, Requests.Join req) {
        leave(sessionId);
        join(sessionId, rooms.findOrCreatePublic(), req);
    }

    public void createPrivate(String sessionId, Requests.Join req) {
        leave(sessionId);
        join(sessionId, rooms.create(true), req);
    }

    public void joinByCode(String sessionId, Requests.Join req) {
        leave(sessionId);
        rooms.find(req == null ? null : req.code()).ifPresentOrElse(
                room -> join(sessionId, room, req),
                () -> out.error(sessionId, "That room doesn't exist. Check the code or create a new room."));
    }

    private void join(String sessionId, GameRoom room, Requests.Join req) {
        synchronized (room) {
            if (room.isClosed()) {
                out.error(sessionId, "That room just closed. Try again.");
                return;
            }
            if (room.getPlayers().size() >= room.getSettings().getMaxPlayers()) {
                out.error(sessionId, "That room is full.");
                return;
            }
            Player p = new Player(UUID.randomUUID().toString().substring(0, 8), sessionId,
                    cleanName(req == null ? null : req.name()), Avatar.sanitize(req == null ? null : req.avatar()));
            room.getPlayers().add(p);
            if (room.getOwnerId() == null) room.setOwnerId(p.getId());
            rooms.bind(sessionId, room.getCode(), p.getId());
            if (room.getPhase().inGame()) room.getDrawQueue().addLast(p.getId()); // draws later this round

            long now = now();
            out.welcome(room, p, now);
            out.chat(room, ChatLine.success(p.getName() + " joined the room!"));
            if (!room.isPrivate() && room.getPhase() == Phase.LOBBY && room.getPlayers().size() >= 2) startGame(room);
            else out.state(room, now);
        }
    }

    public void leave(String sessionId) {
        Binding b = rooms.unbind(sessionId);
        if (b == null) return;
        rooms.find(b.code()).ifPresent(room -> {
            synchronized (room) {
                removePlayer(room, b.playerId(), " left the room.");
            }
        });
    }

    /** Re-send the full room snapshot to this player (used right after subscribing to the room topic). */
    public void sync(String sessionId) {
        withPlayer(sessionId, (room, p) -> out.welcome(room, p, now()));
    }

    private void removePlayer(GameRoom room, String playerId, String what) {
        Player p = room.player(playerId);
        if (p == null) return;
        room.getPlayers().remove(p);
        room.getKickVotes().remove(playerId);
        room.getKickVotes().values().forEach(v -> v.remove(playerId));
        if (room.getPlayers().isEmpty()) {
            rooms.delete(room);
            return;
        }
        if (playerId.equals(room.getOwnerId())) room.setOwnerId(room.getPlayers().get(0).getId());
        out.chat(room, ChatLine.warn(p.getName() + what));

        Phase phase = room.getPhase();
        if (phase.inGame() && room.getPlayers().size() < 2) endGame(room);
        else if (playerId.equals(room.getDrawerId()) && (phase == Phase.CHOOSING || phase == Phase.DRAWING)) endTurn(room, "The drawer left.");
        else if (phase == Phase.DRAWING && allGuessed(room)) endTurn(room, "Everybody guessed the word!");
        else out.state(room, now());
    }

    // ------------------------------------------------------------------ lobby

    public void updateSettings(String sessionId, RoomSettings settings) {
        withPlayer(sessionId, (room, p) -> {
            if (!room.isPrivate() || !p.getId().equals(room.getOwnerId()) || room.getPhase() != Phase.LOBBY || settings == null) return;
            room.setSettings(settings.sanitized(room.getPlayers().size()));
            out.state(room, now());
        });
    }

    public void start(String sessionId) {
        withPlayer(sessionId, (room, p) -> {
            if (!p.getId().equals(room.getOwnerId()) || room.getPhase() != Phase.LOBBY) return;
            if (room.getPlayers().size() < 2) {
                out.error(sessionId, "You need at least 2 players to start. Share the invite link!");
                return;
            }
            startGame(room);
        });
    }

    private void startGame(GameRoom room) {
        room.getPlayers().forEach(pl -> pl.setScore(0));
        room.setRound(0);
        room.getDrawQueue().clear();
        log.info("Game started in room {}", room.getCode());
        nextTurn(room);
    }

    // ------------------------------------------------------------------ turns

    private void nextTurn(GameRoom room) {
        String drawerId = null;
        while (drawerId == null) {
            if (room.getDrawQueue().isEmpty()) {
                if (room.getRound() >= room.getSettings().getRounds()) {
                    endGame(room);
                    return;
                }
                room.setRound(room.getRound() + 1);
                room.setDrawQueue(new ArrayDeque<>(room.getPlayers().stream().map(Player::getId).toList()));
                out.chat(room, ChatLine.system("Round " + room.getRound() + " of " + room.getSettings().getRounds()));
            }
            String id = room.getDrawQueue().pollFirst();
            if (room.player(id) != null) drawerId = id; // skip players who left
        }
        room.resetTurn();
        room.setDrawerId(drawerId);
        RoomSettings s = room.getSettings();
        room.setWordChoices(words.choices(s.getWordCount(), s.getCustomWords(), s.isCustomOnly()));
        room.setPhase(Phase.CHOOSING);
        room.setDeadline(now() + props.getChooseSeconds() * 1000L);

        out.sync(room);
        out.state(room, now());
        out.toPlayer(room.drawer(), Map.of("type", "words", "words", room.getWordChoices()));
    }

    public void chooseWord(String sessionId, int index) {
        withPlayer(sessionId, (room, p) -> {
            if (room.getPhase() != Phase.CHOOSING || !p.getId().equals(room.getDrawerId())) return;
            if (index < 0 || index >= room.getWordChoices().size()) return;
            beginDrawing(room, room.getWordChoices().get(index));
        });
    }

    private void beginDrawing(GameRoom room, String word) {
        long now = now();
        long drawMs = room.getSettings().getDrawTime() * 1000L;
        room.setWord(word);
        room.setPhase(Phase.DRAWING);
        room.setDeadline(now + drawMs);
        room.getHintTimes().addAll(Hints.schedule(now, drawMs, Hints.hintCount(word, room.getSettings().getHints())));

        Player drawer = room.drawer();
        out.toPlayer(drawer, Map.of("type", "word", "word", word));
        out.chat(room, ChatLine.system(drawer.getName() + " is drawing now!"));
        out.state(room, now);
    }

    private void endTurn(GameRoom room, String reason) {
        room.setPhase(Phase.TURN_END);
        room.setDeadline(now() + props.getTurnEndSeconds() * 1000L);
        room.setTurnResult(new TurnResult(room.getWord(), new LinkedHashMap<>(room.getGains()), reason));
        if (room.getWord() != null) out.chat(room, ChatLine.success("The word was '" + room.getWord() + "'"));
        out.state(room, now());
    }

    private void endGame(GameRoom room) {
        room.setPhase(Phase.GAME_OVER);
        room.setDeadline(now() + props.getGameOverSeconds() * 1000L);
        room.setDrawerId(null);
        room.setWord(null);
        room.getDrawQueue().clear();
        room.getPlayers().stream().max((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .ifPresent(w -> out.chat(room, ChatLine.success(w.getName() + " won the game!")));
        out.state(room, now());
    }

    private void afterGameOver(GameRoom room) {
        room.resetTurn();
        room.setDrawerId(null);
        if (!room.isPrivate() && room.getPlayers().size() >= 2) {
            startGame(room);
            return;
        }
        room.setPhase(Phase.LOBBY);
        out.sync(room);
        out.state(room, now());
    }

    // ------------------------------------------------------------------ clock

    @Scheduled(fixedDelay = 250)
    public void tickAll() {
        long now = now();
        for (GameRoom room : rooms.all()) {
            try {
                synchronized (room) {
                    if (!room.isClosed()) tick(room, now);
                }
            } catch (RuntimeException e) {
                log.error("Tick failed for room {}", room.getCode(), e);
            }
        }
    }

    void tick(GameRoom room, long now) {
        switch (room.getPhase()) {
            case CHOOSING -> {
                if (now >= room.getDeadline()) {
                    List<String> choices = room.getWordChoices();
                    beginDrawing(room, choices.get(random.nextInt(choices.size())));
                }
            }
            case DRAWING -> {
                if (now >= room.getDeadline()) {
                    endTurn(room, "Time is up!");
                    return;
                }
                Long next = room.getHintTimes().peekFirst();
                if (next != null && now >= next) {
                    room.getHintTimes().pollFirst();
                    int i = Hints.pickNext(room.getWord(), room.getRevealed(), random);
                    if (i >= 0) room.getRevealed().add(i);
                    out.state(room, now);
                }
            }
            case TURN_END -> {
                if (now >= room.getDeadline()) nextTurn(room);
            }
            case GAME_OVER -> {
                if (now >= room.getDeadline()) afterGameOver(room);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ chat and guessing

    public void chat(String sessionId, String raw) {
        String text = cleanChat(raw);
        if (text.isEmpty()) return;
        withPlayer(sessionId, (room, p) -> {
            boolean drawing = room.getPhase() == Phase.DRAWING && room.getWord() != null;
            if (!drawing) {
                out.chat(room, ChatLine.chat(p, text));
                return;
            }
            String word = room.getWord();
            boolean isDrawer = p.getId().equals(room.getDrawerId());
            if (isDrawer && GuessChecker.revealsWord(text, word)) {
                out.chatTo(p, ChatLine.warn("You can't write the word you're drawing."));
            } else if (isDrawer || p.isGuessed()) {
                // People who know the word talk among themselves so they can't spoil it.
                ChatLine line = ChatLine.guessed(p, text);
                room.getPlayers().stream()
                        .filter(o -> o.isGuessed() || o.getId().equals(room.getDrawerId()))
                        .forEach(o -> out.chatTo(o, line));
            } else if (GuessChecker.isCorrect(text, word)) {
                correctGuess(room, p);
            } else {
                out.chat(room, ChatLine.chat(p, text));
                if (GuessChecker.isClose(text, word)) out.chatTo(p, ChatLine.close("'" + text + "' is close!"));
            }
        });
    }

    private void correctGuess(GameRoom room, Player p) {
        long now = now();
        int points = Scoring.guesser(room.getDeadline() - now, room.getSettings().getDrawTime() * 1000L, room.getGuessedCount());
        int drawerPoints = Scoring.drawerShare(points);
        room.setGuessedCount(room.getGuessedCount() + 1);
        p.setGuessed(true);
        p.setScore(p.getScore() + points);
        room.getGains().merge(p.getId(), points, Integer::sum);
        Player drawer = room.drawer();
        if (drawer != null) {
            drawer.setScore(drawer.getScore() + drawerPoints);
            room.getGains().merge(drawer.getId(), drawerPoints, Integer::sum);
        }
        out.toPlayer(p, Map.of("type", "word", "word", room.getWord()));
        out.chat(room, ChatLine.success(p.getName() + " guessed the word!"));
        if (allGuessed(room)) endTurn(room, "Everybody guessed the word!");
        else out.state(room, now);
    }

    private static boolean allGuessed(GameRoom room) {
        var guessers = room.getPlayers().stream().filter(p -> !p.getId().equals(room.getDrawerId())).toList();
        return !guessers.isEmpty() && guessers.stream().allMatch(Player::isGuessed);
    }

    // ------------------------------------------------------------------ drawing

    public void draw(String sessionId, List<DrawOp> ops) {
        if (ops == null || ops.isEmpty()) return;
        withPlayer(sessionId, (room, p) -> {
            if (!isDrawing(room, p)) return;
            List<DrawOp> clean = new ArrayList<>(ops.size());
            for (DrawOp op : ops) {
                DrawOp c = DrawOp.sanitize(op);
                if (c != null && room.getOps().size() + clean.size() < MAX_OPS) clean.add(c);
            }
            if (clean.isEmpty()) return;
            room.getOps().addAll(clean);
            out.ops(room, clean);
        });
    }

    public void clear(String sessionId) {
        withPlayer(sessionId, (room, p) -> {
            if (!isDrawing(room, p)) return;
            room.getOps().clear();
            out.sync(room);
        });
    }

    /** Removes the last pen gesture (all ops sharing the newest group id). */
    public void undo(String sessionId) {
        withPlayer(sessionId, (room, p) -> {
            if (!isDrawing(room, p) || room.getOps().isEmpty()) return;
            List<DrawOp> list = room.getOps();
            int g = list.get(list.size() - 1).g();
            while (!list.isEmpty() && list.get(list.size() - 1).g() == g) list.remove(list.size() - 1);
            out.sync(room);
        });
    }

    private static boolean isDrawing(GameRoom room, Player p) {
        return room.getPhase() == Phase.DRAWING && p.getId().equals(room.getDrawerId());
    }

    // ------------------------------------------------------------------ social

    public void rate(String sessionId, boolean like) {
        withPlayer(sessionId, (room, p) -> {
            if (room.getPhase() != Phase.DRAWING || p.getId().equals(room.getDrawerId()) || !room.getRaters().add(p.getId())) return;
            Player d = room.drawer();
            if (d == null) return;
            out.chat(room, like ? ChatLine.success(p.getName() + " liked the drawing!") : ChatLine.warn(p.getName() + " disliked the drawing."));
        });
    }

    /** Majority vote kick. Needs at least 2 votes, so nobody can kick alone. */
    public void voteKick(String sessionId, String targetId) {
        withPlayer(sessionId, (room, p) -> {
            Player target = room.player(targetId);
            if (target == null || target == p) return;
            Set<String> votes = room.getKickVotes().computeIfAbsent(targetId, k -> new HashSet<>());
            if (!votes.add(p.getId())) return;
            int needed = votesNeeded(room.getPlayers().size());
            out.chat(room, ChatLine.warn(p.getName() + " is voting to kick " + target.getName() + " (" + votes.size() + "/" + needed + ")"));
            if (votes.size() >= needed) kick(room, target);
        });
    }

    /** The owner of a private room can kick directly. */
    public void ownerKick(String sessionId, String targetId) {
        withPlayer(sessionId, (room, p) -> {
            Player target = room.player(targetId);
            if (!room.isPrivate() || !p.getId().equals(room.getOwnerId()) || target == null || target == p) return;
            kick(room, target);
        });
    }

    static int votesNeeded(int players) {
        return Math.max(2, (int) Math.ceil((players - 1) / 2.0));
    }

    private void kick(GameRoom room, Player target) {
        rooms.unbind(target.getSessionId());
        out.toPlayer(target, Map.of("type", "kicked"));
        removePlayer(room, target.getId(), " was kicked.");
    }

    // ------------------------------------------------------------------ helpers

    private void withPlayer(String sessionId, BiConsumer<GameRoom, Player> action) {
        Binding b = rooms.binding(sessionId);
        if (b == null) return;
        rooms.find(b.code()).ifPresent(room -> {
            synchronized (room) {
                Player p = room.player(b.playerId());
                if (p != null && !room.isClosed()) action.accept(room, p);
            }
        });
    }

    private long now() {
        return clock.millis();
    }

    static String cleanName(String name) {
        String n = name == null ? "" : name.replaceAll("[\\p{Cntrl}]", "").trim().replaceAll("\\s+", " ");
        if (n.length() > 20) n = n.substring(0, 20).trim();
        return n.isEmpty() ? "Player" + (100 + new Random().nextInt(900)) : n;
    }

    static String cleanChat(String text) {
        String t = text == null ? "" : text.replaceAll("[\\p{Cntrl}]", "").trim();
        return t.length() > MAX_CHAT ? t.substring(0, MAX_CHAT) : t;
    }
}
