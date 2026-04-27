package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoundControllerTest {

    private RoundController roundController;

    @BeforeEach
    void setUp() {
        roundController = new RoundController();
    }

    @Test
    void testInitialPhaseIsMrX() {
        assertEquals(TurnType.MRX, roundController.getCurrentPhase());
    }

    @Test
    void testInitialRoundIsOne() {
        assertEquals(1, roundController.getCurrentRound().get());
    }

    @Test
    void testInitiallyMrXTurnIsTrue() {
        assertTrue(roundController.isMrXTurn());
    }

    @Test
    void testInitiallyDetectiveTurnIsFalse() {
        assertFalse(roundController.isDetectiveTurn());
    }

    @Test
    void testInitiallyNoPendingDetectives() {
        assertTrue(roundController.getPendingDetectives().isEmpty());
    }

    @Test
    void testInitDetectives_addsAllDetectiveIds() {
        roundController.initDetectives(Set.of("d1", "d2", "d3"));
        // after MrX moves, all three should be pending
        roundController.recordMrXMove();

        assertEquals(Set.of("d1", "d2", "d3"), roundController.getPendingDetectives());
    }

    @Test
    void testInitDetectives_replacesExistingIds() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.initDetectives(Set.of("d3"));
        roundController.recordMrXMove();

        assertEquals(Set.of("d3"), roundController.getPendingDetectives());
    }

    @Test
    void testAddPendingDetectives_addsToSet() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);
        roundController.addPendingDetectives("d1");

        assertTrue(roundController.isDetectivePending("d1"));
    }

    @Test
    void testAddPendingDetectives_multipleIds() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);
        roundController.addPendingDetectives("d1");
        roundController.addPendingDetectives("d2");

        assertTrue(roundController.getPendingDetectives().contains("d1"));
        assertTrue(roundController.getPendingDetectives().contains("d2"));
    }

    @Test
    void testRecordMrXMove_switchesToDetectivesPhase() {
        roundController.recordMrXMove();

        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());
        assertTrue(roundController.isDetectiveTurn());
        assertFalse(roundController.isMrXTurn());
    }

    @Test
    void testRecordMrXMove_addsAllDetectiveIdsToPendingIds() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.recordMrXMove();

        assertEquals(Set.of("d1", "d2"), roundController.getPendingDetectives());
    }

    @Test
    void testRecordMrXMove_clearsPreviousPendingDetectives() {
        roundController.addPendingDetectives("nonExistent");
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();

        assertFalse(roundController.isDetectivePending("nonExistent"));
        assertTrue(roundController.isDetectivePending("d1"));
    }

    @Test
    void testRecordMrXMove_duringDetectivesPhase() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> roundController.recordMrXMove());

        assertTrue(ex.getMessage().contains("Cannot record Mr. X move"));
    }

    @Test
    void testRecordMrXMove_doesNotIncrementRound() {
        roundController.recordMrXMove();

        assertEquals(1, roundController.getCurrentRound().get());
    }

    @Test
    void testRecordMrXMove_resetsMovesRemainingToOne() {
        roundController.recordMrXMove();

        assertEquals(1, roundController.getMrxMovesRemaining());   // reset to 1 for next round
    }

    @Test
    void testActivateDoubleMove_setsMrXMovesRemainingToTwo() {
        roundController.activateDoubleMove();

        assertEquals(2, roundController.getMrxMovesRemaining());
    }

    @Test
    void testActivateDoubleMove_setsDoubleMoveActiveTrue() {
        roundController.activateDoubleMove();

        assertTrue(roundController.isDoubleMoveActive());
    }

    @Test
    void testActivateDoubleMove_duringDetectivesPhase() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);

        IllegalStateException ex = assertThrows(IllegalStateException.class, roundController::activateDoubleMove);
        assertTrue(ex.getMessage().contains("Cannot activate double move"));
    }

    @Test
    void testActivateDoubleMove_whenAlreadyActive() {
        roundController.activateDoubleMove();

        IllegalStateException ex = assertThrows(IllegalStateException.class, roundController::activateDoubleMove);
        assertTrue(ex.getMessage().contains("already in use"));
    }

    @Test
    void testDoubleMove_phaseStaysMRX() {
        roundController.activateDoubleMove();
        roundController.recordMrXMove();  // first of two moves

        assertEquals(TurnType.MRX, roundController.getCurrentPhase());
        assertTrue(roundController.isMrXTurn());
    }

    @Test
    void testDoubleMove_movesRemainingDecrementsToOne() {
        roundController.activateDoubleMove();
        roundController.recordMrXMove();

        assertEquals(1, roundController.getMrxMovesRemaining());
    }

    @Test
    void testDoubleMove_firstRecordMrXMove_doubleMoveStillActive() {
        roundController.activateDoubleMove();
        roundController.recordMrXMove();

        assertTrue(roundController.isDoubleMoveActive());
    }

    @Test
    void testDoubleMove_secondRecordMrXMove_switchesToDetectives() {
        roundController.initDetectives(Set.of("d1"));
        roundController.activateDoubleMove();
        roundController.recordMrXMove();  // first move – phase stays MRX
        roundController.recordMrXMove();  // second move – phase advances

        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());
    }

    @Test
    void testDoubleMove_doubleMoveActiveResetsFalse() {
        roundController.initDetectives(Set.of("d1"));
        roundController.activateDoubleMove();
        roundController.recordMrXMove();
        roundController.recordMrXMove();

        assertFalse(roundController.isDoubleMoveActive());
    }

    @Test
    void testDoubleMove_movesRemainingResetsToOne() {
        roundController.initDetectives(Set.of("d1"));
        roundController.activateDoubleMove();
        roundController.recordMrXMove();
        roundController.recordMrXMove();

        assertEquals(1, roundController.getMrxMovesRemaining());
    }

    @Test
    void testDoubleMove_detectiveCannotMoveBetweenMrXMoves() {
        roundController.initDetectives(Set.of("d1"));
        roundController.activateDoubleMove();
        roundController.recordMrXMove();  // still MRX phase

        // canMove for a detective must be false while phase is MRX
        assertFalse(roundController.canMove("d1", false));
    }

    @Test
    void testDoubleMove_mrXCanMoveForBothMoves() {
        roundController.activateDoubleMove();
        assertTrue(roundController.canMove("mrx", true));   // before first move

        roundController.recordMrXMove();
        assertTrue(roundController.canMove("mrx", true));   // before second move

        roundController.recordMrXMove();
        assertFalse(roundController.canMove("mrx", true));   // after second move
    }

    @Test
    void testDoubleMove_RoundAdvancesCorrectly() {
        roundController.initDetectives(Set.of("d1"));
        assertEquals(1, roundController.getCurrentRound().get());

        roundController.activateDoubleMove();
        roundController.recordMrXMove();
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertEquals(2, roundController.getCurrentRound().get());
        assertEquals(TurnType.MRX, roundController.getCurrentPhase());
    }

    @Test
    void testRecordDetectiveMove_removesPendingDetective() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertFalse(roundController.isDetectivePending("d1"));
        assertTrue(roundController.isDetectivePending("d2"));
    }

    @Test
    void testRecordDetectiveMove_lastDetective() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertEquals(2, roundController.getCurrentRound().get());
        assertEquals(TurnType.MRX, roundController.getCurrentPhase());
        assertTrue(roundController.isMrXTurn());
    }

    @Test
    void testRecordDetectiveMove_notAllDetectivesMoved() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());
        assertEquals(1, roundController.getCurrentRound().get());
    }

    @Test
    void testRecordDetectiveMove_duringMrXPhase() {
        // phase is MRX by default
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> roundController.recordDetectiveMove("d1"));
        assertTrue(ex.getMessage().contains("Cannot record detective move"));
    }

    @Test
    void testRecordDetectiveMove_detectiveNotPending() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();
        // d2 was never registered
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> roundController.recordDetectiveMove("d2"));
        assertTrue(ex.getMessage().contains("d2"));
    }

    @Test
    void testRecordDetectiveMove_sameDetectiveTwice() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> roundController.recordDetectiveMove("d1"));
        assertTrue(ex.getMessage().contains("d1"));
    }

    @Test
    void testFullRound_twoDetectivesRoundAdvancesCorrectly() {
        roundController.initDetectives(Set.of("d1", "d2"));

        // Round 1, MrX moves
        roundController.recordMrXMove();
        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());

        // Round 1, Detectives move
        roundController.recordDetectiveMove("d1");
        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());
        roundController.recordDetectiveMove("d2");

        // now should be round 2
        assertEquals(2, roundController.getCurrentRound().get());
        assertEquals(TurnType.MRX, roundController.getCurrentPhase());
    }

    @Test
    void testMultipleRounds_roundCounterIncrementsCorrectly() {
        roundController.initDetectives(Set.of("d1"));

        for (int expectedRound = 1; expectedRound <= 5; expectedRound++) {
            assertEquals(expectedRound, roundController.getCurrentRound().get());
            roundController.recordMrXMove();
            roundController.recordDetectiveMove("d1");
        }
        assertEquals(6, roundController.getCurrentRound().get());
    }

    @Test
    void testIsDetectivePending_unknownId() {
        assertFalse(roundController.isDetectivePending("nonExistent"));
    }

    @Test
    void testIsDetectivePending_afterMove() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertFalse(roundController.isDetectivePending("d1"));
    }

    @Test
    void testCanMove_mrxSuccess() {
        assertTrue(roundController.canMove("mrx", true));
    }

    @Test
    void testCanMove_mrxDuringDetectivesPhase() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);

        assertFalse(roundController.canMove("mrx", true));
    }

    @Test
    void testCanMove_detectiveSuccess() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();

        assertTrue(roundController.canMove("d1", false));
    }

    @Test
    void testCanMove_detectiveDuringMrXPhase() {
        assertFalse(roundController.canMove("d1", false));
    }

    @Test
    void testCanMove_detectiveAlreadyMoved() {
        roundController.initDetectives(Set.of("d1", "d2"));
        roundController.recordMrXMove();
        roundController.recordDetectiveMove("d1");

        assertFalse(roundController.canMove("d1", false));
    }

    @Test
    void testCanMove_detectiveNotRegistered() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();

        assertFalse(roundController.canMove("notRegistered", false));
    }

    @Test
    void testGetPendingDetectives_returnsUnmodifiableSet() {
        roundController.initDetectives(Set.of("d1"));
        roundController.recordMrXMove();
        Set<String> pending = roundController.getPendingDetectives();

        assertThrows(UnsupportedOperationException.class, () -> pending.add("d2"));
    }

    @Test
    void testSetCurrentPhase() {
        roundController.setCurrentPhase(TurnType.DETECTIVES);

        assertEquals(TurnType.DETECTIVES, roundController.getCurrentPhase());
        assertTrue(roundController.isDetectiveTurn());
        assertFalse(roundController.isMrXTurn());
    }
}