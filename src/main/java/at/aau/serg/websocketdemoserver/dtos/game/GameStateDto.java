package at.aau.serg.websocketdemoserver.dtos.game;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GameStateDto {
    private String gameId;
    private int currentRound;
    private TurnType currentPhase;
    private Map<String, Integer> detectivePositions;
    private Integer mrXPosition;
    private boolean doubleMoveActive;
    private int mrxMovesRemaining;
    private Map<String, Map<String, Integer>> playerTickets;    // playerId -> (ticketType -> count)
    private Map<String, Integer> mrXSpecialTickets;             // only BLACK and DOUBLE
}