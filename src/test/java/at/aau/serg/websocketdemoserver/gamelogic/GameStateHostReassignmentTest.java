package at.aau.serg.websocketdemoserver.gamelogic;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameState.reassignHostIfNeeded(), which promotes a new host when
 * the current host disconnects and at least one other player remains.
 */
class GameStateHostReassignmentTest {

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
    void testReassignHostIfNeeded_disconnectedPlayerIsNotHost_doesNothing() {
        Set<String> connected = Set.of("host1", "det2", "mrx1");

        String newHost = gameState.reassignHostIfNeeded("det1", connected);

        assertNull(newHost);
        assertEquals("host1", gameState.getHostId());
    }

    @Test
    void testReassignHostIfNeeded_nullDisconnectedId_doesNothing() {
        String newHost = gameState.reassignHostIfNeeded(null, Set.of("det1", "det2", "mrx1"));

        assertNull(newHost);
        assertEquals("host1", gameState.getHostId());
    }

    @Test
    void testReassignHostIfNeeded_hostDisconnects_promotesTheOnlyConnectedPlayer() {
        // Only det2 is still connected - det1 and mrx1 are also offline.
        Set<String> connected = Set.of("det2");

        String newHost = gameState.reassignHostIfNeeded("host1", connected);

        assertEquals("det2", newHost);
        assertEquals("det2", gameState.getHostId());
    }

    @Test
    void testReassignHostIfNeeded_hostDisconnects_neverPromotesTheDisconnectedHostItself() {
        // Edge case: the "connected" set erroneously still contains the host -
        // must never be picked as its own successor.
        Set<String> connected = Set.of("host1", "det1");

        String newHost = gameState.reassignHostIfNeeded("host1", connected);

        assertEquals("det1", newHost);
        assertNotEquals("host1", newHost);
    }

    @Test
    void testReassignHostIfNeeded_noOneConnected_fallsBackToAnyOtherRemainingPlayer() {
        // Nobody is reported as connected (e.g. all subscriptions dropped at once) -
        // still better to promote *someone* than to leave the game without a host.
        String newHost = gameState.reassignHostIfNeeded("host1", Collections.emptySet());

        assertNotNull(newHost);
        assertNotEquals("host1", newHost);
        assertTrue(Set.of("det1", "det2", "mrx1").contains(newHost));
        assertEquals(newHost, gameState.getHostId());
    }

    @Test
    void testReassignHostIfNeeded_singlePlayerGame_returnsNullAndKeepsHost() {
        // Kick everyone else out first, so the host is the only player left.
        gameState.kickPlayer("host1", "det1");
        gameState.kickPlayer("host1", "det2");
        gameState.kickPlayer("host1", "mrx1");

        String newHost = gameState.reassignHostIfNeeded("host1", Collections.emptySet());

        assertNull(newHost);
        assertEquals("host1", gameState.getHostId());
    }

    @Test
    void testReassignHostIfNeeded_prefersConnectedPlayerOverDisconnectedOnes() {
        // det1 and mrx1 are technically still players but offline; only det2 is
        // actually connected - it must be preferred over the offline candidates.
        Set<String> connected = Set.of("det2");

        String newHost = gameState.reassignHostIfNeeded("host1", connected);

        assertEquals("det2", newHost);
    }
}
