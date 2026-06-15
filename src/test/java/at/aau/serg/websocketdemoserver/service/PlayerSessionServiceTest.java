package at.aau.serg.websocketdemoserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerSessionServiceTest {

    private PlayerSessionService service;

    @BeforeEach
    void setUp() {
        service = new PlayerSessionService();
    }

    @Test
    void testPlayerJoinedLobby() {
        service.playerJoinedLobby("user1", "lobby1");
        assertEquals("lobby1", service.getLobbyForPlayer("user1"));
        assertTrue(service.isPlayerInLobby("user1"));
    }

    @Test
    void testPlayerJoinedGame() {
        service.playerJoinedGame("user1", "game1");
        assertEquals("game1", service.getGameForPlayer("user1"));
        assertTrue(service.isPlayerInGame("user1"));
    }

    @Test
    void testPlayerLeftLobby() {
        service.playerJoinedLobby("user1", "lobby1");
        service.playerLeftLobby("user1");
        assertNull(service.getLobbyForPlayer("user1"));
        assertFalse(service.isPlayerInLobby("user1"));
    }

    @Test
    void testPlayerLeftGame() {
        service.playerJoinedGame("user1", "game1");
        service.playerLeftGame("user1");
        assertNull(service.getGameForPlayer("user1"));
        assertFalse(service.isPlayerInGame("user1"));
    }

    @Test
    void testPlayerFullyLeft() {
        service.playerJoinedLobby("user1", "lobby1");
        service.playerJoinedGame("user1", "game1");
        service.playerFullyLeft("user1");
        assertNull(service.getLobbyForPlayer("user1"));
        assertNull(service.getGameForPlayer("user1"));
    }

    @Test
    void testGetLobbyForUnknownUser() {
        assertNull(service.getLobbyForPlayer("unknown"));
        assertFalse(service.isPlayerInLobby("unknown"));
    }

    @Test
    void testGetGameForUnknownUser() {
        assertNull(service.getGameForPlayer("unknown"));
        assertFalse(service.isPlayerInGame("unknown"));
    }

    @Test
    void testOverwriteLobbyId() {
        service.playerJoinedLobby("user1", "lobby1");
        service.playerJoinedLobby("user1", "lobby2");
        assertEquals("lobby2", service.getLobbyForPlayer("user1"));
    }

    @Test
    void testPlayerDisconnected() {
        service.playerJoinedLobby("user1", "lobby1");
        service.playerJoinedGame("user1", "game1");
        service.playerDisconnected("user1");
        // playerDisconnected should not crash
        assertNotNull(service);
    }
}