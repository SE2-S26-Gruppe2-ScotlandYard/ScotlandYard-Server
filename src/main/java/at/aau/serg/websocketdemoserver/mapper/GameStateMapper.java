package at.aau.serg.websocketdemoserver.mapper;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;

public class GameStateMapper {

    private GameStateMapper() {}

    public static GameStateDto toDto(GameState gameState) {
        at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType phase = gameState.getCurrentPhase();
        return new GameStateDto(
                gameState.getGameId(),
                gameState.getCurrentRound(),
                phase,
                phase == at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType.MRX,
                phase == at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType.DETECTIVES,
                gameState.getDetectivePositions(),
                gameState.getMrXPosition(),
                gameState.getRoundController().isDoubleMoveActive(),
                gameState.getRoundController().getMrxMovesRemaining(),
                gameState.getPlayerTickets(),
                gameState.getMrXSpecialTickets(),
                gameState.getMrXMoveHistory(),
                gameState.getMrXRevealedPositions()
        );
    }
}