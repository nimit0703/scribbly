package com.scribb.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scribb.game.config.GameProperties;
import com.scribb.game.model.Avatar;
import com.scribb.game.model.ChatLine;
import com.scribb.game.model.DrawOp;
import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Phase;
import com.scribb.game.model.Player;
import com.scribb.game.model.Requests;
import com.scribb.game.model.RoomSettings;
import com.scribb.game.repository.InMemoryGameRoomRepository;

/**
 * Drives whole games through the engine with a fake clock: no sockets, no sleeping.
 * Sessions are "a", "b", "c"...; the broadcaster is a mock we verify against.
 */
class GameEngineTest {

    /** A clock we move by hand. */
    static class FakeClock extends Clock {
        long millis = 1_000_000;
        @Override public long millis() { return millis; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        void advanceSeconds(double s) { millis += (long) (s * 1000); }
    }

    FakeClock clock;
    GameProperties props;
    RoomManager rooms;
    GameBroadcaster out;
    GameEngine engine;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        props = new GameProperties();
        rooms = new RoomManager(new InMemoryGameRoomRepository(), props);
        out = mock(GameBroadcaster.class);
        WordBankService words = new WordBankService(props);
        words.load();
        engine = new GameEngine(rooms, out, words, props, clock);
    }

    // ------------------------------------------------------------------ helpers

    static Requests.Join as(String name) {
        return new Requests.Join(name, new Avatar(1, 2, 3), null);
    }

    GameRoom roomOf(String session) {
        return rooms.find(rooms.binding(session).code()).orElseThrow();
    }

    Player me(String session) {
        return roomOf(session).player(rooms.binding(session).playerId());
    }

    /** Private room with the given sessions; the first one is the owner. */
    GameRoom privateRoom(String... sessions) {
        engine.createPrivate(sessions[0], as(sessions[0].toUpperCase()));
        String code = roomOf(sessions[0]).getCode();
        for (int i = 1; i < sessions.length; i++) {
            engine.joinByCode(sessions[i], new Requests.Join(sessions[i].toUpperCase(), null, code.toLowerCase()));
        }
        return roomOf(sessions[0]);
    }

    String drawerSession(GameRoom room, String... sessions) {
        for (String s : sessions) if (me(s).getId().equals(room.getDrawerId())) return s;
        throw new AssertionError("no drawer");
    }

    void tick(GameRoom room) {
        engine.tick(room, clock.millis());
    }

    // ------------------------------------------------------------------ lobby

    @Test
    void privateRoomWaitsForOwnerToStart() {
        GameRoom room = privateRoom("a", "b");
        assertThat(room.getPlayers()).hasSize(2);
        assertThat(room.getOwnerId()).isEqualTo(me("a").getId());
        assertThat(room.getPhase()).isEqualTo(Phase.LOBBY);

        engine.start("b"); // not the owner
        assertThat(room.getPhase()).isEqualTo(Phase.LOBBY);

        engine.start("a");
        assertThat(room.getPhase()).isEqualTo(Phase.CHOOSING);
        assertThat(room.getRound()).isEqualTo(1);
        assertThat(room.getDrawerId()).isEqualTo(me("a").getId()); // join order
        assertThat(room.getWordChoices()).hasSize(3);
        verify(out).toPlayer(eq(me("a")), argThat(m -> "words".equals(m.get("type"))));
    }

    @Test
    void ownerCannotStartAlone() {
        GameRoom room = privateRoom("a");
        engine.start("a");
        assertThat(room.getPhase()).isEqualTo(Phase.LOBBY);
        verify(out).error(eq("a"), any());
    }

    @Test
    void onlyOwnerChangesSettingsAndOnlyInLobby() {
        GameRoom room = privateRoom("a", "b");
        RoomSettings s = new RoomSettings();
        s.setRounds(5);
        s.setCustomWords(List.of("zebra crossing", "moonwalk", "selfie"));
        s.setCustomOnly(true);

        engine.updateSettings("b", s);
        assertThat(room.getSettings().getRounds()).isEqualTo(3);

        engine.updateSettings("a", s);
        assertThat(room.getSettings().getRounds()).isEqualTo(5);

        engine.start("a");
        assertThat(room.getWordChoices()).containsExactlyInAnyOrder("zebra crossing", "moonwalk", "selfie");

        s.setRounds(2);
        engine.updateSettings("a", s);
        assertThat(room.getSettings().getRounds()).isEqualTo(5);
    }

    @Test
    void unknownCodeAndFullRoomAreRejected() {
        engine.joinByCode("x", new Requests.Join("X", null, "NOPE42"));
        verify(out).error(eq("x"), argThat(m -> m.contains("doesn't exist")));

        GameRoom room = privateRoom("a", "b");
        room.getSettings().setMaxPlayers(2);
        engine.joinByCode("c", new Requests.Join("C", null, room.getCode()));
        verify(out).error(eq("c"), argThat(m -> m.contains("full")));
        assertThat(room.getPlayers()).hasSize(2);
    }

    @Test
    void quickPlayFillsOnePublicRoomAndStartsAtTwoPlayers() {
        engine.quickPlay("a", as("A"));
        GameRoom room = roomOf("a");
        assertThat(room.isPrivate()).isFalse();
        assertThat(room.getPhase()).isEqualTo(Phase.LOBBY);

        engine.quickPlay("b", as("B"));
        assertThat(roomOf("b")).isSameAs(room);
        assertThat(room.getPhase()).isEqualTo(Phase.CHOOSING);

        engine.quickPlay("c", as("C")); // joins mid-game and is queued to draw this round
        assertThat(room.getDrawQueue()).contains(me("c").getId());
    }

    // ------------------------------------------------------------------ a turn

    @Test
    void fullTurnWithGuessesScoringAndHints() {
        GameRoom room = privateRoom("a", "b", "c");
        room.getSettings().setHints(2);
        room.getSettings().setDrawTime(90);
        engine.start("a");
        engine.chooseWord("b", 0); // not the drawer: ignored
        assertThat(room.getPhase()).isEqualTo(Phase.CHOOSING);

        engine.chooseWord("a", 1);
        String word = room.getWordChoices().get(1);
        assertThat(room.getPhase()).isEqualTo(Phase.DRAWING);
        assertThat(room.getWord()).isEqualTo(word);
        verify(out).toPlayer(eq(me("a")), argThat(m -> "word".equals(m.get("type")) && word.equals(m.get("word"))));
        assertThat(GameBroadcaster.view(room, clock.millis()).hint()).doesNotContainPattern("[a-z]");

        // a hint appears a third of the way in (if the word is long enough to get one)
        clock.advanceSeconds(31);
        tick(room);
        if (Hints.hintCount(word, 2) > 0) assertThat(room.getRevealed()).hasSize(1);

        // a wrong guess is public chat
        engine.chat("b", "definitely not it");
        verify(out).chat(eq(room), argThat(l -> "chat".equals(l.kind()) && l.text().equals("definitely not it")));

        // a correct guess scores, tells the guesser the word and is not shown as chat
        clock.advanceSeconds(29); // 30s of 90 left
        engine.chat("b", word.toUpperCase());
        Player b = me("b");
        assertThat(b.isGuessed()).isTrue();
        assertThat(b.getScore()).isEqualTo(Scoring.guesser(30_000, 90_000, 0));
        assertThat(me("a").getScore()).isEqualTo(Scoring.drawerShare(b.getScore()));
        verify(out).toPlayer(eq(b), argThat(m -> word.equals(m.get("word"))));
        verify(out, never()).chat(eq(room), argThat(l -> "chat".equals(l.kind()) && l.text().equalsIgnoreCase(word)));
        assertThat(room.getPhase()).isEqualTo(Phase.DRAWING);

        // last guesser ends the turn early
        engine.chat("c", word);
        assertThat(room.getPhase()).isEqualTo(Phase.TURN_END);
        assertThat(room.getTurnResult().word()).isEqualTo(word);
        assertThat(room.getTurnResult().gains()).containsKeys(me("a").getId(), b.getId(), me("c").getId());
        assertThat(me("c").getScore()).isLessThan(b.getScore()); // second guesser gets a smaller order bonus

        // next turn after the result screen: b draws
        clock.advanceSeconds(props.getTurnEndSeconds());
        tick(room);
        assertThat(room.getPhase()).isEqualTo(Phase.CHOOSING);
        assertThat(room.getDrawerId()).isEqualTo(b.getId());
        assertThat(room.getOps()).isEmpty();
        assertThat(me("b").isGuessed()).isFalse();
    }

    @Test
    void closeGuessIsWhisperedToTheGuesserOnly() {
        GameRoom room = privateRoom("a", "b");
        room.getSettings().setCustomWords(List.of("elephant"));
        room.getSettings().setCustomOnly(true);
        room.getSettings().setWordCount(1);
        engine.start("a");
        engine.chooseWord("a", 0);

        engine.chat("b", "elephent");
        verify(out).chatTo(eq(me("b")), argThat(l -> "close".equals(l.kind())));
        verify(out).chat(eq(room), argThat(l -> "elephent".equals(l.text())));
    }

    @Test
    void drawerCannotTypeTheWordAndGuessedChatIsPrivate() {
        GameRoom room = privateRoom("a", "b", "c");
        engine.start("a");
        engine.chooseWord("a", 0);
        String word = room.getWord();

        engine.chat("a", "it is " + word);
        verify(out).chatTo(eq(me("a")), argThat(l -> "warn".equals(l.kind())));
        verify(out, never()).chat(eq(room), argThat(l -> l.text().contains(word) && "chat".equals(l.kind())));

        engine.chat("b", word);
        engine.chat("b", "so easy");
        // only the drawer and b (who guessed) see it; c is still guessing
        verify(out).chatTo(eq(me("a")), argThat(l -> "guessed".equals(l.kind()) && "so easy".equals(l.text())));
        verify(out).chatTo(eq(me("b")), argThat(l -> "guessed".equals(l.kind()) && "so easy".equals(l.text())));
        verify(out, never()).chatTo(eq(me("c")), argThat(l -> "so easy".equals(l.text())));
    }

    @Test
    void timeoutsPickAWordAndEndTheTurn() {
        GameRoom room = privateRoom("a", "b");
        engine.start("a");
        clock.advanceSeconds(props.getChooseSeconds());
        tick(room);
        assertThat(room.getPhase()).isEqualTo(Phase.DRAWING);
        assertThat(room.getWordChoices()).contains(room.getWord());

        clock.advanceSeconds(room.getSettings().getDrawTime());
        tick(room);
        assertThat(room.getPhase()).isEqualTo(Phase.TURN_END);
        assertThat(room.getTurnResult().reason()).isEqualTo("Time is up!");
        assertThat(room.getTurnResult().gains()).isEmpty();
    }

    @Test
    void wholeGameEndsWithPodiumThenLobby() {
        GameRoom room = privateRoom("a", "b");
        room.getSettings().setRounds(2);
        engine.start("a");
        int turns = 0;
        while (room.getPhase() != Phase.GAME_OVER) {
            assertThat(turns++).isLessThan(10);
            engine.chooseWord(drawerSession(room, "a", "b"), 0);
            String guesser = drawerSession(room, "a", "b").equals("a") ? "b" : "a";
            engine.chat(guesser, room.getWord());
            clock.advanceSeconds(props.getTurnEndSeconds());
            tick(room);
        }
        assertThat(turns).isEqualTo(4); // 2 rounds x 2 players
        assertThat(room.getRound()).isEqualTo(2);
        verify(out, atLeastOnce()).chat(eq(room), argThat(l -> l.text().endsWith("won the game!")));

        clock.advanceSeconds(props.getGameOverSeconds());
        tick(room);
        assertThat(room.getPhase()).isEqualTo(Phase.LOBBY);
        assertThat(me("a").getScore()).isPositive(); // scores stay visible until the next game

        engine.start("a");
        assertThat(me("a").getScore()).isZero();
    }

    // ------------------------------------------------------------------ drawing

    @Test
    void onlyTheDrawerDrawsAndUndoRemovesAWholeStroke() {
        GameRoom room = privateRoom("a", "b");
        engine.start("a");
        engine.chooseWord("a", 0);

        engine.draw("b", List.of(new DrawOp("s", 1, "#000000", 4, new int[]{1, 1, 2, 2})));
        assertThat(room.getOps()).isEmpty();

        engine.draw("a", List.of(
                new DrawOp("s", 1, "#000000", 4, new int[]{1, 1, 2, 2}),
                new DrawOp("s", 1, "#000000", 4, new int[]{2, 2, 3, 3}),
                new DrawOp("bogus", 1, "#000000", 4, new int[]{1, 1})));
        engine.draw("a", List.of(new DrawOp("f", 2, "#ff0000", 0, new int[]{50, 50})));
        assertThat(room.getOps()).hasSize(3);

        engine.undo("a");
        assertThat(room.getOps()).hasSize(2).allMatch(op -> op.g() == 1);
        engine.undo("a");
        assertThat(room.getOps()).isEmpty();

        engine.draw("a", List.of(new DrawOp("s", 3, "#000000", 4, new int[]{1, 1, 2, 2})));
        engine.clear("a");
        assertThat(room.getOps()).isEmpty();
        verify(out, atLeastOnce()).sync(room);
    }

    // ------------------------------------------------------------------ leaving and kicking

    @Test
    void drawerLeavingEndsTheTurnAndLastPlayerEndsTheGame() {
        GameRoom room = privateRoom("a", "b", "c");
        engine.start("a");
        engine.chooseWord("a", 0);

        engine.leave("a");
        assertThat(room.getPhase()).isEqualTo(Phase.TURN_END);
        assertThat(room.getTurnResult().reason()).isEqualTo("The drawer left.");
        assertThat(room.getOwnerId()).isEqualTo(me("b").getId()); // ownership moves on

        engine.leave("b");
        assertThat(room.getPhase()).isEqualTo(Phase.GAME_OVER);

        engine.leave("c");
        assertThat(room.isClosed()).isTrue();
        assertThat(rooms.find(room.getCode())).isEmpty();
    }

    @Test
    void lastGuesserLeavingEndsTheTurn() {
        GameRoom room = privateRoom("a", "b", "c");
        engine.start("a");
        engine.chooseWord("a", 0);
        engine.chat("b", room.getWord());
        engine.leave("c");
        assertThat(room.getPhase()).isEqualTo(Phase.TURN_END);
    }

    @Test
    void voteKickNeedsTwoVotes() {
        GameRoom room = privateRoom("a", "b", "c", "d");
        String target = me("d").getId();
        engine.voteKick("a", target);
        engine.voteKick("a", target); // same voter twice counts once
        assertThat(room.player(target)).isNotNull();

        engine.voteKick("b", target);
        assertThat(room.player(target)).isNull();
        assertThat(rooms.binding("d")).isNull();
    }

    @Test
    void ownerKicksDirectlyOthersCannot() {
        GameRoom room = privateRoom("a", "b", "c");
        engine.ownerKick("b", me("c").getId());
        assertThat(room.getPlayers()).hasSize(3);
        String c = me("c").getId();
        engine.ownerKick("a", c);
        assertThat(room.player(c)).isNull();
    }

    @Test
    void likesAreCountedOncePerPlayerPerTurn() {
        GameRoom room = privateRoom("a", "b");
        engine.start("a");
        engine.chooseWord("a", 0);
        engine.rate("a", true); // drawer can't rate themselves
        engine.rate("b", true);
        engine.rate("b", true);
        verify(out).chat(eq(room), argThat((ChatLine l) -> l.text().endsWith("liked the drawing!")));
    }

    @Test
    void switchingRoomsLeavesTheOldOne() {
        GameRoom first = privateRoom("a", "b");
        engine.createPrivate("b", as("B"));
        assertThat(first.getPlayers()).hasSize(1);
        assertThat(roomOf("b")).isNotSameAs(first);
    }
}
