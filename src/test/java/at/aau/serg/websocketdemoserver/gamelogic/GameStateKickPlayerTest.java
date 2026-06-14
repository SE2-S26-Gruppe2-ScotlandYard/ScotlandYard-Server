package at.aau.serg.websocketdemoserver.gamelogic;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameStateKickPlayerTest {

    private GameState gameState;
    private Lobby mockLobby;
    private User host;
    private User detective1;
    private User detective2;
    private User mrX;

    @BeforeEach
    void setUp() {
        gameState = new GameState("game1");
        host = new User("host1", "Host");
        detective1 = new User("det1", "Detective1");
        detective2 = new User("det2", "Detective2");
        mrX = new User("mrx1", "MrX");

        mockLobby = mock(Lobby.class);
        when(mockLobby.getHostId()).thenReturn("host1");
        when(mockLobby.getUsers()).thenReturn(Arrays.asList(host, detective1, detective2, mrX));
        when(mockLobby.getSelectedRole("host1")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("det1")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("det2")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("mrx1")).thenReturn(Role.MRX);

        gameState.initializePlayersFromLobby(mockLobby);
    }

    @Test
    void testHostIdIsSetFromLobby() {
        assertEquals("host1", gameState.getHostId());
    }

    @Test
    void testGetPlayerNamesReturnsAllPlayers() {
        Map<String, String> names = gameState.getPlayerNames();
        assertEquals(4, names.size());
        assertEquals("Host", names.get("host1"));
        assertEquals("Detective1", names.get("det1"));
        assertEquals("MrX", names.get("mrx1"));
    }

    @Test
    void testKickPlayerByNonHostThrows() {
        assertThrows(IllegalStateException.class,
                () -> gameState.kickPlayer("det1", "det2"));
    }

    @Test
    void testKickUnknownPlayerThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> gameState.kickPlayer("host1", "unknown"));
    }

    @Test
    void testKickMrXReturnsMrXKicked() {
        String result = gameState.kickPlayer("host1", "mrx1");
        assertEquals("MRX_KICKED", result);
    }

    @Test
    void testKickDetectiveContinuesGame() {
        String result = gameState.kickPlayer("host1", "det1");
        assertEquals("CONTINUE", result);
    }

    @Test
    void testKickPlayerRemovesFromPlayerNames() {
        gameState.kickPlayer("host1", "det1");
        assertFalse(gameState.getPlayerNames().containsKey("det1"));
    }

    @Test
    void testKickUntilTooFewPlayers() {
        // Start with 4: host, det1, det2, mrx1
        // Kick 2 detectives, then kick host (still detective in role)
        // After 3 kicks, only mrX remains -> < 2 players
        gameState.kickPlayer("host1", "det1");
        gameState.kickPlayer("host1", "det2");
        String result = gameState.kickPlayer("host1", "host1");
        assertEquals("TOO_FEW_PLAYERS", result);
    }
}