package com.example.Poker.game;

import com.example.Poker.dto.MoveDTO;
import com.example.Poker.dto.PokerDTO;
import com.example.Poker.dto.HandDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokerTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static MoveDTO call()               { return new MoveDTO("CALL",   null); }
    private static MoveDTO check()              { return new MoveDTO("CHECK",  null); }
    private static MoveDTO fold()               { return new MoveDTO("FOLD",   null); }
    private static MoveDTO raise(int amount)    { return new MoveDTO("RAISE",  amount); }
    private static MoveDTO allIn()              { return new MoveDTO("ALLIN",  null); }

    /** Returns the name of whichever player is currently speaking. */
    private static String speaker(Poker game) {
        return game.toDto().speaking();
    }

    /** Drives every non-folded player to CHECK until the round advances or the
     *  hand ends.  Stops after {@code maxActions} actions to prevent infinite
     *  loops in broken tests. */
    private static void checkAround(Poker game, int maxActions) {
        for (int i = 0; i < maxActions; i++) {
            if (game.shouldFinishHand()) break;
            String sp = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, check());
            if (err != Poker.PokerError.SUCCESS && err != Poker.PokerError.PLAYER_CANNOT_CHECK) break;
        }
    }

    // ── construction ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Game initialises with correct number of players")
        void playerCount() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            assertEquals(3, game.getActivePlayers());
        }

        @Test
        @DisplayName("Pot contains small blind + big blind after setup")
        void initialPot() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            PokerDTO dto = game.toDto();
            // SB = 15, BB = 30
            assertTrue(dto.pot() >= 45,
                    "Pot should be at least SB+BB (45) after setup, was " + dto.pot());
        }

        @Test
        @DisplayName("Copy constructor produces independent deep copy")
        void copyConstructor() {
            Poker original = new Poker(List.of("Alice", "Bob", "Carol"));
            Poker copy     = new Poker(original);

            // Advance original; copy must not change
            String origSpeaker = speaker(original);
            original.handleMessage(origSpeaker, fold());

            assertEquals(3, copy.getActivePlayers(),
                    "Copy should still have 3 active players after original was mutated");
        }

        @Test
        @DisplayName("Copy constructor throws on null source")
        void copyConstructorNull() {
            assertThrows(IllegalArgumentException.class, () -> new Poker((Poker) null));
        }
    }

    // ── player queries ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Player queries")
    class PlayerQueries {

        private Poker game;

        @BeforeEach
        void setUp() {
            game = new Poker(List.of("Alice", "Bob", "Carol"));
        }

        @Test
        @DisplayName("playerIsPlaying returns true for known player")
        void playerIsPlayingTrue() {
            assertTrue(game.playerIsPlaying("Alice"));
        }

        @Test
        @DisplayName("playerIsPlaying returns false for unknown player")
        void playerIsPlayingFalse() {
            assertFalse(game.playerIsPlaying("Nobody"));
        }

        @Test
        @DisplayName("getPlayerNames returns all player names")
        void getPlayerNames() {
            List<String> names = game.getPlayerNames();
            assertTrue(names.containsAll(List.of("Alice", "Bob", "Carol")));
            assertEquals(3, names.size());
        }

        @Test
        @DisplayName("getPlayerHand returns cards for active player")
        void getPlayerHandActive() {
            HandDTO hand = game.getPlayerHand("Alice");
            // At least one card should be non-null for an active player
            assertTrue(hand.first() != null || hand.second() != null,
                    "Active player should have cards");
        }

        @Test
        @DisplayName("getPlayerHand returns null cards for unknown player")
        void getPlayerHandUnknown() {
            HandDTO hand = game.getPlayerHand("Nobody");
            assertNull(hand.first());
            assertNull(hand.second());
        }
    }

    // ── move validation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Move validation")
    class MoveValidation {

        private Poker game;

        @BeforeEach
        void setUp() {
            game = new Poker(List.of("Alice", "Bob", "Carol"));
        }

        @Test
        @DisplayName("Non-speaking player gets MESSAGE_REQUESTED_BY_NON_SPEAKING")
        void nonSpeakingPlayer() {
            String sp    = speaker(game);
            String other = game.getPlayerNames().stream()
                               .filter(n -> !n.equals(sp)).findFirst().orElseThrow();

            Poker.PokerError err = game.handleMessage(other, check());
            assertEquals(Poker.PokerError.MESSAGE_REQUESTED_BY_NON_SPEAKING, err);
        }

        @Test
        @DisplayName("RAISE without amount returns RAISE_AMOUNT_IS_NULL")
        void raiseWithoutAmount() {
            String sp  = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, new MoveDTO("RAISE", null));
            assertEquals(Poker.PokerError.RAISE_AMOUNT_IS_NULL, err);
        }

        @Test
        @DisplayName("CHECK when bet is below max returns PLAYER_CANNOT_CHECK")
        void checkWhenBehind() {
            // In pre-flop the first speaker is UTG, who hasn't matched the BB yet
            String sp = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, check());
            assertEquals(Poker.PokerError.PLAYER_CANNOT_CHECK, err);
        }

        @Test
        @DisplayName("Successful CALL returns SUCCESS")
        void callSuccess() {
            String sp  = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, call());
            assertEquals(Poker.PokerError.SUCCESS, err);
        }

        @Test
        @DisplayName("Successful RAISE returns SUCCESS")
        void raiseSuccess() {
            String sp  = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, raise(10));
            assertEquals(Poker.PokerError.SUCCESS, err);
        }

        @Test
        @DisplayName("FOLD returns SUCCESS and reduces active players")
        void foldSuccess() {
            String sp     = speaker(game);
            int before    = game.getActivePlayers();
            Poker.PokerError err = game.handleMessage(sp, fold());

            assertEquals(Poker.PokerError.SUCCESS, err);
            assertEquals(before - 1, game.getActivePlayers());
        }

        @Test
        @DisplayName("ALLIN returns SUCCESS")
        void allInSuccess() {
            String sp  = speaker(game);
            Poker.PokerError err = game.handleMessage(sp, allIn());
            assertEquals(Poker.PokerError.SUCCESS, err);
        }
    }

    // ── betting round progression ─────────────────────────────────────────────

    @Nested
    @DisplayName("Betting round progression")
    class BettingRoundProgression {

        @Test
        @DisplayName("Community cards are dealt as betting rounds advance")
        void communityCardsDealt() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));

            // Pre-flop: everyone calls/checks
            drivePreFlop(game);

            PokerDTO dto = game.toDto();
            long dealtCount = java.util.Arrays.stream(dto.communityCards())
                                              .filter(c -> c != null)
                                              .count();
            assertTrue(dealtCount >= 3, "At least the flop should be on the table");
        }

        @Test
        @DisplayName("Speaking player changes after a move")
        void speakerAdvances() {
            Poker game  = new Poker(List.of("Alice", "Bob", "Carol"));
            String before = speaker(game);
            game.handleMessage(before, call());
            String after  = speaker(game);
            // speaker must have changed (or hand ended)
            assertTrue(!before.equals(after) || game.shouldFinishHand());
        }

        /** Drives the pre-flop betting round to completion. */
        private void drivePreFlop(Poker game) {
            // UTG calls, next player calls, BB checks (or similar)
            for (int i = 0; i < 10; i++) {
                if (game.shouldFinishHand()) break;
                PokerDTO dto = game.toDto();
                long dealt = java.util.Arrays.stream(dto.communityCards())
                                             .filter(c -> c != null).count();
                if (dealt >= 3) break; // flop out — pre-flop done

                String sp  = speaker(game);
                Poker.PokerError err = game.handleMessage(sp, call());
                if (err == Poker.PokerError.PLAYER_SHOULD_CHECK) {
                    game.handleMessage(sp, check());
                }
            }
        }
    }

    // ── shouldFinishHand ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("shouldFinishHand")
    class ShouldFinishHand {

        @Test
        @DisplayName("Hand finishes when all but one player folds")
        void finishesOnFold() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));

            // Fold until one player remains
            for (int i = 0; i < 10; i++) {
                if (game.getActivePlayers() == 1) break;
                String sp  = speaker(game);
                game.handleMessage(sp, fold());
            }

            assertTrue(game.shouldFinishHand());
        }

        @Test
        @DisplayName("Hand does not finish at game start")
        void doesNotFinishAtStart() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            assertFalse(game.shouldFinishHand());
        }
    }

    // ── removePlayer ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removePlayer")
    class RemovePlayer {

        @Test
        @DisplayName("Removed player is no longer playing")
        void removedPlayerGone() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            // Remove a non-speaking player to avoid index shifting issues in-move
            String nonSpeaker = game.getPlayerNames().stream()
                                    .filter(n -> !n.equals(speaker(game)))
                                    .findFirst().orElseThrow();
            game.removePlayer(nonSpeaker);
            assertFalse(game.playerIsPlaying(nonSpeaker));
        }

        @Test
        @DisplayName("Game continues after player removal")
        void gameContainsAfterRemoval() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            String nonSpeaker = game.getPlayerNames().stream()
                                    .filter(n -> !n.equals(speaker(game)))
                                    .findFirst().orElseThrow();
            game.removePlayer(nonSpeaker);
            // Should still be able to get a valid speaker
            assertNotNull(speaker(game));
            assertTrue(game.playerIsPlaying(speaker(game)));
        }
    }

    // ── getStatesToFinishHand ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getStatesToFinishHand")
    class StatesToFinishHand {

        @Test
        @DisplayName("Returns two equal-length lists")
        void returnsTwoLists() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            List<List<Poker>> states = game.getStatesToFinishHand();
            assertEquals(2, states.size());
            assertEquals(states.get(0).size(), states.get(1).size());
        }

        @Test
        @DisplayName("Returned states are independent copies (mutations don't cross)")
        void statesAreIndependent() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            List<List<Poker>> states = game.getStatesToFinishHand();

            Poker prev0 = states.get(0).get(0);
            Poker next0 = states.get(1).get(0);

            // They should not be the same object
            assertNotSame(prev0, next0);
        }

        @Test
        @DisplayName("When one player remains, exactly two state pairs returned")
        void oneActivePlayer() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));

            // Fold down to one active player
            for (int i = 0; i < 5; i++) {
                if (game.getActivePlayers() == 1) break;
                String sp = speaker(game);
                game.handleMessage(sp, fold());
            }

            assertEquals(1, game.getActivePlayers());
            List<List<Poker>> states = game.getStatesToFinishHand();
            assertEquals(2, states.get(0).size());
        }
    }

    // ── toDto ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDto")
    class ToDto {

        @Test
        @DisplayName("toDto returns correct round count")
        void roundCount() {
            Poker game = new Poker(List.of("Alice", "Bob", "Carol"));
            assertEquals(1, game.toDto().round());
        }

        @Test
        @DisplayName("toDto(observer) hides other players' cards")
        void observerHidesCards() {
            Poker game     = new Poker(List.of("Alice", "Bob", "Carol"));
            PokerDTO dto   = game.toDto("Alice");

            dto.players().forEach(p -> {
                if (!p.name().equals("Alice")) {
                    // Non-observed players should have hidden (null) cards
                    assertNull(p.hand().first(), "Other player's first card should be hidden");
                    assertNull(p.hand().second(), "Other player's second card should be hidden");
                }
            });
        }
    }
}
