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
        gameController = GameController.getInstance();
        mockGameState = mock(GameState.class);
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
    void testGetGameExisting() {
        String gameId = "game1";
        gameController.addGame(gameId, mockGameState);

        GameState retrievedGame = gameController.getGame(gameId);

        assertNotNull(retrievedGame);
        assertEquals(mockGameState, retrievedGame);
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
}