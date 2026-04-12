package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Manages the round of a Scotland Yard game.
 * Implemented thread safety to let the Detectives move as they wish.
 *
 * 1. Round starts as MRX turn.
 *    - Once MRX has moved, the phase automatically advances to DETECTIVES turn.
 * 2. During DETECTIVES turn every detective may move in any order.
 *    - Each detective may only move once per round.
 *    - After the last pending detective has moved, recordDetectiveMove(String) advances the phase back to MRX turn and increments the round counter.
 */
public class RoundController {

    @Getter
    private final AtomicInteger currentRound = new AtomicInteger(1);    // using Atomic Integer to increment counter safely

    @Getter     // using volatile to ensure variable is correctly read by all threads
    @Setter
    private volatile TurnType currentPhase = TurnType.MRX;      // MRX or DETECTIVES

    private final Set<String> pendingDetectives = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<String> allDetectiveIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    public void addPendingDetectives(String playerId) {
        pendingDetectives.add(playerId);
    }

    // record/save made moves in this round

    public synchronized void recordMrXMove() {
        if (currentPhase != TurnType.MRX) {
            throw new IllegalStateException(
                    "Cannot record Mr. X move: current phase is " + currentPhase);
        }
        //when Mr. X has moved, Detectives are allowed to move
        pendingDetectives.clear();
        pendingDetectives.addAll(allDetectiveIds);
        currentPhase = TurnType.DETECTIVES;
    }

    public synchronized void recordDetectiveMove(String detectiveId) {
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
            currentRound.incrementAndGet();
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