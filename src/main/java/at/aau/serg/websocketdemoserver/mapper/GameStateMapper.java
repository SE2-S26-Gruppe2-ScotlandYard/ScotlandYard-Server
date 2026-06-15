package at.aau.serg.websocketdemoserver.mapper;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;

import java.util.Collections;
import java.util.Set;

public class GameStateMapper {
    private GameStateMapper() {}

    public static GameStateDto toDto(GameState gameState) {
        return toDto(gameState, Collections.emptySet());
    }

    public static GameStateDto toDto(GameState gameState, Set<String> disconnectedPlayers) {
        return new GameStateDto(
                gameState.getGameId(),
                gameState.getCurrentRound(),
                gameState.getHostId(),
                gameState.getCurrentPhase(),
                gameState.getDetectivePositions(),
                gameState.getMrXPosition(),
                gameState.getRoundController().isDoubleMoveActive(),
                gameState.getRoundController().getMrxMovesRemaining(),
                gameState.getPlayerTickets(),
                gameState.getMrXSpecialTickets(),
                gameState.getMrXMoveHistory(),
                gameState.getMrXRevealedPositions(),
                gameState.allPlayersHaveStartPosition(),
                gameState.getPlayerNames(),
                disconnectedPlayers
        );
    }
}