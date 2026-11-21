package com.scribb.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.scribb.game.model.DrawOp;
import com.scribb.game.model.RoomSettings;

/** Pure rule helpers: hints, guess matching, scoring, input sanitising. */
class RulesTest {

    @Nested
    class HintsTest {
        @Test
        void maskHidesLettersButKeepsSpacesAndHyphens() {
            assertThat(Hints.mask("ice cream", Set.of())).isEqualTo("___ _____");
            assertThat(Hints.mask("yo-yo", Set.of(0))).isEqualTo("y_-__");
        }

        @Test
        void neverRevealsMoreThanHalfTheLetters() {
            assertThat(Hints.hintCount("cat", 5)).isEqualTo(1);
            assertThat(Hints.hintCount("elephant", 2)).isEqualTo(2);
            assertThat(Hints.hintCount("elephant", 0)).isZero();
            assertThat(Hints.hintCount("ice cream", 9)).isEqualTo(4); // 8 letters, the space doesn't count
        }

        @Test
        void revealsAreEvenlySpacedInsideTheTurn() {
            assertThat(Hints.schedule(1000, 90_000, 2)).containsExactly(31_000L, 61_000L);
            assertThat(Hints.schedule(0, 60_000, 0)).isEmpty();
        }

        @Test
        void pickNextOnlyPicksHiddenLettersAndStopsWhenAllShown() {
            Set<Integer> revealed = new HashSet<>();
            Random r = new Random(42);
            for (int i = 0; i < 6; i++) { // "hot dog" has 6 letters
                int idx = Hints.pickNext("hot dog", revealed, r);
                assertThat(idx).isNotEqualTo(3); // the space
                assertThat(revealed.add(idx)).isTrue();
            }
            assertThat(revealed).hasSize(6);
            assertThat(Hints.pickNext("hot dog", revealed, r)).isEqualTo(-1);
        }
    }

    @Nested
    class GuessCheckerTest {
        @Test
        void correctIgnoresCaseSpacesAndPunctuation() {
            assertThat(GuessChecker.isCorrect("Apple", "apple")).isTrue();
            assertThat(GuessChecker.isCorrect("  ICE-CREAM ", "ice cream")).isTrue();
            assertThat(GuessChecker.isCorrect("icecream", "ice cream")).isTrue();
            assertThat(GuessChecker.isCorrect("apples", "apple")).isFalse();
            assertThat(GuessChecker.isCorrect("", "apple")).isFalse();
            assertThat(GuessChecker.isCorrect("!!!", "")).isFalse();
        }

        @Test
        void closeMeansExactlyOneTypo() {
            assertThat(GuessChecker.isClose("elephent", "elephant")).isTrue(); // substitution
            assertThat(GuessChecker.isClose("elphant", "elephant")).isTrue();  // deletion
            assertThat(GuessChecker.isClose("elephants", "elephant")).isTrue(); // insertion
            assertThat(GuessChecker.isClose("elephant", "elephant")).isFalse(); // correct, not close
            assertThat(GuessChecker.isClose("elefent", "elephant")).isFalse();  // two away
        }

        @Test
        void shortWordsAreNeverClose() {
            assertThat(GuessChecker.isClose("cap", "cat")).isFalse();
        }

        @Test
        void revealsWordCatchesTheWordInsideASentence() {
            assertThat(GuessChecker.revealsWord("it's a Hot-Dog lol", "hot dog")).isTrue();
            assertThat(GuessChecker.revealsWord("good luck", "hot dog")).isFalse();
        }

        @Test
        void levenshtein() {
            assertThat(GuessChecker.levenshtein("kitten", "sitting")).isEqualTo(3);
            assertThat(GuessChecker.levenshtein("", "abc")).isEqualTo(3);
        }
    }

    @Nested
    class ScoringTest {
        @Test
        void fasterAndEarlierGuessesScoreMore() {
            assertThat(Scoring.guesser(80_000, 80_000, 0)).isEqualTo(500);
            assertThat(Scoring.guesser(40_000, 80_000, 1)).isEqualTo(50 + 175 + 50);
            assertThat(Scoring.guesser(0, 80_000, 5)).isEqualTo(50);
            assertThat(Scoring.guesser(-5, 80_000, 5)).isEqualTo(50); // clock skew never goes negative
        }

        @Test
        void drawerGetsHalf() {
            assertThat(Scoring.drawerShare(275)).isEqualTo(137);
        }
    }

    @Nested
    class SanitizeTest {
        @Test
        void settingsAreClampedAndCustomWordsCleaned() {
            RoomSettings in = new RoomSettings();
            in.setRounds(99);
            in.setDrawTime(1);
            in.setMaxPlayers(1);
            in.setHints(-3);
            in.setCustomWords(List.of(" Pizza ", "pizza", "", "  hot   dog ", "x".repeat(31)));
            RoomSettings s = in.sanitized(4);
            assertThat(s.getRounds()).isEqualTo(10);
            assertThat(s.getDrawTime()).isEqualTo(15);
            assertThat(s.getMaxPlayers()).isEqualTo(4); // can't go below the people already in the room
            assertThat(s.getHints()).isZero();
            assertThat(s.getCustomWords()).containsExactly("pizza", "hot dog");
        }

        @Test
        void drawOpsAreValidatedAndClamped() {
            assertThat(DrawOp.sanitize(new DrawOp("s", 1, "#FF0000", 999, new int[]{-5, 10, 900, 700})))
                    .satisfies(op -> {
                        assertThat(op.p()).containsExactly(0, 10, 800, 600);
                        assertThat(op.s()).isEqualTo(60);
                        assertThat(op.c()).isEqualTo("#ff0000");
                    });
            assertThat(DrawOp.sanitize(new DrawOp("s", 1, "red", 5, new int[]{1, 2}))).isNull();          // bad colour
            assertThat(DrawOp.sanitize(new DrawOp("s", 1, "#000000", 5, new int[]{1, 2, 3}))).isNull();   // odd length
            assertThat(DrawOp.sanitize(new DrawOp("f", 1, "#000000", 5, new int[]{1, 2, 3, 4}))).isNull(); // fill takes one point
            assertThat(DrawOp.sanitize(new DrawOp("x", 1, "#000000", 5, new int[]{1, 2}))).isNull();      // unknown type
        }

        @Test
        void namesAndChatAreTrimmed() {
            assertThat(GameEngine.cleanName("  Bob\u0007  the   Builder ")).isEqualTo("Bob the Builder");
            assertThat(GameEngine.cleanName("   ")).startsWith("Player");
            assertThat(GameEngine.cleanName("a".repeat(40))).hasSize(20);
            assertThat(GameEngine.cleanChat("x".repeat(500))).hasSize(GameEngine.MAX_CHAT);
        }

        @Test
        void kickNeedsAtLeastTwoVotes() {
            assertThat(GameEngine.votesNeeded(2)).isEqualTo(2);
            assertThat(GameEngine.votesNeeded(3)).isEqualTo(2);
            assertThat(GameEngine.votesNeeded(8)).isEqualTo(4);
        }
    }
}
