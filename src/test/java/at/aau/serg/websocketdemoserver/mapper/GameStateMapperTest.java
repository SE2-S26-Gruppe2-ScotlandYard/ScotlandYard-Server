package at.aau.serg.websocketdemoserver.mapper;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.service.RoundController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameStateMapperTest {

    private GameState mockGameState;
    private RoundController mockRoundController;

    @BeforeEach
    void setUp() {
        mockGameState = mock(GameState.class);
        mockRoundController = mock(RoundController.class);

        when(mockGameState.getGameId()).thenReturn("game1");
        when(mockGameState.getCurrentRound()).thenReturn(3);
        when(mockGameState.getCurrentPhase()).thenReturn(TurnType.DETECTIVES);
        when(mockGameState.getDetectivePositions()).thenReturn(Map.of("det1", 42, "det2", 77));
        when(mockGameState.getMrXPosition()).thenReturn(99);
        when(mockGameState.getRoundController()).thenReturn(mockRoundController);
        when(mockRoundController.isDoubleMoveActive()).thenReturn(false);
        when(mockRoundController.getMrxMovesRemaining()).thenReturn(1);
        when(mockGameState.getPlayerTickets()).thenReturn(Map.of("det1", Map.of("WALKING", 9), "mrx1", Map.of("BLACK", 4))        );
        when(mockGameState.getMrXSpecialTickets()).thenReturn(Map.of("BLACK", 4, "DOUBLE", 2));
        when(mockGameState.getMrXMoveHistory()).thenReturn(List.of("WALKING", "ESCOOTER"));
        when(mockGameState.getMrXRevealedPositions()).thenReturn(Map.of(3, 99));
        when(mockGameState.allPlayersHaveStartPosition()).thenReturn(true);
    }

    @Test
    void testToDto_returnsNonNull() {
        assertNotNull(GameStateMapper.toDto(mockGameState));
    }

    @Test
    void testToDto_gameId() {
        assertEquals("game1", GameStateMapper.toDto(mockGameState).getGameId());
    }

    @Test
    void testToDto_currentRound() {
        assertEquals(3, GameStateMapper.toDto(mockGameState).getCurrentRound());
    }

    @Test
    void testToDto_currentPhase() {
        assertEquals(TurnType.DETECTIVES, GameStateMapper.toDto(mockGameState).getCurrentPhase());
    }

    @Test
    void testToDto_detectivePositions() {
        assertEquals(Map.of("det1", 42, "det2", 77), GameStateMapper.toDto(mockGameState).getDetectivePositions());
    }

    @Test
    void testToDto_mrXPosition() {
        assertEquals(99, GameStateMapper.toDto(mockGameState).getMrXPosition());
    }

    @Test
    void testToDto_doubleMoveActive_false() {
        assertFalse(GameStateMapper.toDto(mockGameState).isDoubleMoveActive());
    }

    @Test
    void testToDto_doubleMoveActive_true() {
        when(mockRoundController.isDoubleMoveActive()).thenReturn(true);
        assertTrue(GameStateMapper.toDto(mockGameState).isDoubleMoveActive());
    }

    @Test
    void testToDto_mrxMovesRemaining_one() {
        assertEquals(1, GameStateMapper.toDto(mockGameState).getMrxMovesRemaining());
    }

    @Test
    void testToDto_mrxMovesRemaining_two() {
        when(mockRoundController.getMrxMovesRemaining()).thenReturn(2);
        assertEquals(2, GameStateMapper.toDto(mockGameState).getMrxMovesRemaining());
    }

    @Test
    void testToDto_playerTickets_containsDetective() {
        GameStateDto dto = GameStateMapper.toDto(mockGameState);
        assertTrue(dto.getPlayerTickets().containsKey("det1"));
        assertEquals(9, dto.getPlayerTickets().get("det1").get("WALKING"));
    }

    @Test
    void testToDto_mrXSpecialTickets() {
        GameStateDto dto = GameStateMapper.toDto(mockGameState);
        assertEquals(4, dto.getMrXSpecialTickets().get("BLACK"));
        assertEquals(2, dto.getMrXSpecialTickets().get("DOUBLE"));
    }

    @Test
    void testToDto_mrXMoveHistory() {
        assertEquals(List.of("WALKING", "ESCOOTER"),
                GameStateMapper.toDto(mockGameState).getMrXMoveHistory());
    }

    @Test
    void testToDto_mrXRevealedPositions() {
        assertEquals(Map.of(3, 99),
                GameStateMapper.toDto(mockGameState).getMrXRevealedPositions());
    }

    @Test
    void testToDto_nullMrXPosition() {
        when(mockGameState.getMrXPosition()).thenReturn(null);
        assertNull(GameStateMapper.toDto(mockGameState).getMrXPosition());
    }

    @Test
    void testToDto_emptyMoveHistory() {
        when(mockGameState.getMrXMoveHistory()).thenReturn(List.of());
        assertTrue(GameStateMapper.toDto(mockGameState).getMrXMoveHistory().isEmpty());
    }

    @Test
    void testToDto_emptyDetectivePositions() {
        when(mockGameState.getDetectivePositions()).thenReturn(Map.of());
        assertTrue(GameStateMapper.toDto(mockGameState).getDetectivePositions().isEmpty());
    }

    @Test
    void testToDto_callsAllRequiredMethods() {
        GameStateMapper.toDto(mockGameState);

        verify(mockGameState).getGameId();
        verify(mockGameState).getCurrentRound();
        verify(mockGameState).getCurrentPhase();
        verify(mockGameState).getDetectivePositions();
        verify(mockGameState).getMrXPosition();
        verify(mockGameState, times(2)).getRoundController(); // isDoubleMoveActive + getMrxMovesRemaining
        verify(mockGameState).getPlayerTickets();
        verify(mockGameState).getMrXSpecialTickets();
        verify(mockGameState).getMrXMoveHistory();
        verify(mockGameState).getMrXRevealedPositions();
        verify(mockGameState).allPlayersHaveStartPosition();
    }

    @Test
    void testToDto_allPlayersReady_true() {
        when(mockGameState.allPlayersHaveStartPosition()).thenReturn(true);
        assertTrue(GameStateMapper.toDto(mockGameState).isAllPlayersReady());
    }

    @Test
    void testToDto_allPlayersReady_false() {
        when(mockGameState.allPlayersHaveStartPosition()).thenReturn(false);
        assertFalse(GameStateMapper.toDto(mockGameState).isAllPlayersReady());
    }

}