package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;


/*
 * Manages the round of a Scotland Yard game.
 *
 * 1. Round starts as MRX turn.
 *    - Once MRX has moved, the phase automatically advances to DETECTIVES turn.
 * 2. During DETECTIVES turn every detective may move in any order.
 *    - Each detective may only move once per round.
 *    - After the last pending detective has moved, recordDetectiveMove(String) advances the phase back to MRX turn and increments the round counter.
 */
public class RoundController {

    @Getter
    private int currentRound = 1;

    @Getter
    private TurnType currentPhase = TurnType.MRX;      // MRX or DETECTIVES

    private final Set<String> pendingDetectives = Collections.newSetFromMap(new HashMap<>());

    private final Set<String> allDetectiveIds = Collections.newSetFromMap(new HashMap<>());

    public void initDetectives(Set<String> detectiveIds) {
        allDetectiveIds.clear();
        allDetectiveIds.addAll(detectiveIds);
    }

    // turn phases

    public boolean isMrXTurn() {
        return currentPhase == TurnType.MRX;
    }

    public boolean isDetectiveTurn() {
        return currentPhase == TurnType.DETECTIVES;
    }

    public boolean isDetectivePending(String detectiveId) {
        return pendingDetectives.contains(detectiveId);
    }

    public Set<String> getPendingDetectives() {
        return Collections.unmodifiableSet(pendingDetectives);
    }

    // record/save made moves in this round

    public void recordMrXMove() {
        if (currentPhase != TurnType.MRX) {
            throw new IllegalStateException(
                    "Cannot record Mr. X move: current phase is " + currentPhase);
        }
        //when Mr. X has moved, Detectives are allowed to move
        pendingDetectives.clear();
        pendingDetectives.addAll(allDetectiveIds);
        currentPhase = TurnType.DETECTIVES;
    }

    public void recordDetectiveMove(String detectiveId) {
        if (currentPhase != TurnType.DETECTIVES) {
            throw new IllegalStateException(
                    "Cannot record detective move: current phase is " + currentPhase);
        }
        if (!pendingDetectives.contains(detectiveId)) {
            throw new IllegalStateException(
                    "Detective " + detectiveId + " has already moved this round or is not registered.");
        }

        pendingDetectives.remove(detectiveId);

        // when every detective has moved, advance to the next round
        if (pendingDetectives.isEmpty()) {
            currentRound++;
            currentPhase = TurnType.MRX;
        }
    }

    // validation helper method
    public boolean canMove(String playerId, boolean isMrX) {
        if (isMrX) {
            return currentPhase == TurnType.MRX;
        } else {
            return currentPhase == TurnType.DETECTIVES
                    && pendingDetectives.contains(playerId);
        }
    }
}