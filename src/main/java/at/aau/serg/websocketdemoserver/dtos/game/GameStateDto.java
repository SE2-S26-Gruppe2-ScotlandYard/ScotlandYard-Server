package at.aau.serg.websocketdemoserver.dtos.game;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameStateDto {
    private String gameId;
    private int currentRound;
    private TurnType currentPhase;
    /** Convenience flag: true when it is Mr. X's turn. */
    private boolean isMrXPhase;
    /** Convenience flag: true when it is the detectives' turn. */
    private boolean isDetectivesPhase;
    private Map<String, Integer> detectivePositions;
    private Integer mrXPosition;
    private boolean doubleMoveActive;
    private int mrxMovesRemaining;
    private Map<String, Map<String, Integer>> playerTickets;    // playerId -> (ticketType -> count)
    private Map<String, Integer> mrXSpecialTickets;             // only BLACK and DOUBLE
    private List<String> mrXMoveHistory;
    private Map<Integer, Integer> mrXRevealedPositions;
}