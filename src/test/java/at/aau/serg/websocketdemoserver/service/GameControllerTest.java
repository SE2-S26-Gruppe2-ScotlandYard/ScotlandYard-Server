package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameControllerTest {

    private GameController gameController;
    private GameState mockGameState;

    @BeforeEach
    void setUp() {
        gameController = new GameController();
        mockGameState = mock(GameState.class);
        when(mockGameState.getGameId()).thenReturn("mockedGame");
    }

    @Test
    void testAddGame() {
        String gameId = "game1";

        gameController.addGame(gameId, mockGameState);

        GameState retrievedGame = gameController.getGame(gameId);
        assertNotNull(retrievedGame);
        assertEquals(mockGameState, retrievedGame);
    }

    @Test
    void testAddMultipleGames() {
        String gameId1 = "game1";
        String gameId2 = "game2";
        GameState mockGameState2 = mock(GameState.class);

        gameController.addGame(gameId1, mockGameState);
        gameController.addGame(gameId2, mockGameState2);

        assertEquals(mockGameState, gameController.getGame(gameId1));
        assertEquals(mockGameState2, gameController.getGame(gameId2));
    }

    @Test
    void testAddGameOverwritesExisting() {
        String gameId = "game1";
        GameState oldGameState = mock(GameState.class);
        GameState newGameState = mock(GameState.class);

        gameController.addGame(gameId, oldGameState);
        gameController.addGame(gameId, newGameState);

        GameState retrievedGame = gameController.getGame(gameId);
        assertEquals(newGameState, retrievedGame);
        assertNotEquals(oldGameState, retrievedGame);
    }

    @Test
    void testGetGameNonExistent() {
        GameState retrievedGame = gameController.getGame("nonExistentId");

        assertNull(retrievedGame);
    }

    @Test
    void testGetGameWithNullId() {
        assertThrows(NullPointerException.class, () -> gameController.getGame(null));
    }

    @Test
    void testRemoveGameExisting() {
        String gameId = "game1";
        gameController.addGame(gameId, mockGameState);

        gameController.removeGame(gameId);

        assertNull(gameController.getGame(gameId));
    }

    @Test
    void testRemoveGameNonExistent_doesNotThrow() {
        assertDoesNotThrow(() -> gameController.removeGame("doesNotExist"));
    }

    @Test
    void testRemoveGameWithNullId() {
        assertThrows(NullPointerException.class, () -> gameController.removeGame(null));
    }

    @Test
    void testAddRemoveGameMultipleTimes() {
        String gameId = "game1";

        gameController.addGame(gameId, mockGameState);
        gameController.removeGame(gameId);
        gameController.addGame(gameId, mockGameState);

        assertNotNull(gameController.getGame(gameId));
        assertEquals(mockGameState, gameController.getGame(gameId));
    }

    @Test
    void testAddGameWithNullGameState() {
        String gameId = "game1";

        assertThrows(NullPointerException.class, () -> gameController.addGame(gameId, null));
    }

    @Test
    void testAddGameWithNullId() {
        assertThrows(NullPointerException.class, () -> gameController.addGame(null, mockGameState));
    }

    @Test
    void testGetGame_afterRemoveAll_returnsNull() {
        gameController.addGame("game1", mockGameState);
        gameController.addGame("game2", mockGameState);
        gameController.removeGame("game1");
        gameController.removeGame("game2");

        assertNull(gameController.getGame("game1"));
        assertNull(gameController.getGame("game2"));
    }

    @Test
    void testGetActiveGamesReturnsEmptyMapInitially() {
        assertTrue(gameController.getActiveGames().isEmpty());
    }

    @Test
    void testGetActiveGamesReturnsAllAddedGames() {
        GameState mockGameState2 = mock(GameState.class);
        gameController.addGame("game1", mockGameState);
        gameController.addGame("game2", mockGameState2);

        var activeGames = gameController.getActiveGames();

        assertEquals(2, activeGames.size());
        assertEquals(mockGameState, activeGames.get("game1"));
        assertEquals(mockGameState2, activeGames.get("game2"));
    }

    @Test
    void testGetActiveGamesReflectsRemoval() {
        gameController.addGame("game1", mockGameState);
        gameController.removeGame("game1");

        assertTrue(gameController.getActiveGames().isEmpty());
    }

    @Test
    void testGetActiveGamesIsUnmodifiable() {
        gameController.addGame("game1", mockGameState);
        var activeGames = gameController.getActiveGames();

        assertThrows(UnsupportedOperationException.class, () -> activeGames.put("game2", mockGameState));
    }
}