package at.aau.serg.websocketdemoserver.gamelogic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;

class GameStateTest {

    private GameState gameState;
    private Lobby mockLobby;
    private User hostUser;
    private User detectiveUser1;
    private User detectiveUser2;
    private User mrXUser;

    @BeforeEach
    void setUp() {
        gameState = new GameState("game123");

        hostUser = new User("user1", "HostPlayer", "pw1");
        detectiveUser1 = new User("user2", "Detective1", "pw2");
        detectiveUser2 = new User("user3", "Detective2", "pw3");
        mrXUser = new User("user4", "MrXPlayer", "pw4");

        mockLobby = mock(Lobby.class);
    }

    @Test
    void testConstructor() {
        assertNotNull(gameState);
        assertEquals("game123", gameState.getGameId());
        assertNotNull(gameState.getBoard());
    }

    @Test
    void testInitializeFromLobbyWhenLobbyCannotStart() {
        when(mockLobby.canStartGame()).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gameState.initializeFromLobby(mockLobby);
        });

        assertEquals("Lobby is not ready to start the game", exception.getMessage());
    }

    @Test
    void testInitializeFromLobbySuccess() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, mrXUser));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        assertNotNull(gameState.getPlayer(hostUser.id()));
        assertNotNull(gameState.getPlayer(detectiveUser1.id()));
        assertNotNull(gameState.getPlayer(mrXUser.id()));

        assertFalse(gameState.getPlayer(hostUser.id()).isMrX());
        assertFalse(gameState.getPlayer(detectiveUser1.id()).isMrX());
        assertTrue(gameState.getPlayer(mrXUser.id()).isMrX());
    }

    @Test
    void testInitializeStartPositions() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2, mrXUser));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        assertNotNull(gameState.getPlayerPosition(hostUser.id()));
        assertNotNull(gameState.getPlayerPosition(detectiveUser1.id()));
        assertNotNull(gameState.getPlayerPosition(detectiveUser2.id()));
        assertNotNull(gameState.getPlayerPosition(mrXUser.id()));

        assertTrue(gameState.getPlayerPosition(hostUser.id()) >= 1 &&
                gameState.getPlayerPosition(hostUser.id()) <= 199);
    }

    @Test
    void testSetPlayerPositionValid() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 42);

        assertEquals(42, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testSetPlayerPositionInvalidBelowRange() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            gameState.setPlayerPosition(hostUser.id(), 0);
        });

        assertEquals("Position must be between 1 and 199", exception.getMessage());
    }

    @Test
    void testSetPlayerPositionInvalidAboveRange() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            gameState.setPlayerPosition(hostUser.id(), 200);
        });

        assertEquals("Position must be between 1 and 199", exception.getMessage());
    }

    @Test
    void testSetPlayerPositionUnknownPlayer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            gameState.setPlayerPosition("unknownPlayer", 42);
        });

        assertEquals("Player not found: unknownPlayer", exception.getMessage());
    }

    @Test
    void testGetMrXPosition() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser, detectiveUser1));
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);
        Integer mrXPosition = gameState.getMrXPosition();

        assertNotNull(mrXPosition);
        assertTrue(mrXPosition >= 1 && mrXPosition <= 199);
    }

    @Test
    void testGetMrXPositionNoMrX() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);
        Integer mrXPosition = gameState.getMrXPosition();

        assertNull(mrXPosition);
    }

    @Test
    void testMultiplePlayersSamePosition() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 100);
        gameState.setPlayerPosition(detectiveUser1.id(), 100);

        assertEquals(100, gameState.getPlayerPosition(hostUser.id()));
        assertEquals(100, gameState.getPlayerPosition(detectiveUser1.id()));
    }

    @Test
    void testUpdatePlayerPosition() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 10);
        assertEquals(10, gameState.getPlayerPosition(hostUser.id()));

        gameState.setPlayerPosition(hostUser.id(), 50);
        assertEquals(50, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testStartPositionsAreValidStationNumbers() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        int[] validStartPositions = new int[] {13, 26, 29, 34, 50, 53, 91, 94, 103, 112, 117, 132, 138, 141, 155, 174, 197, 198};

        for (User user : List.of(hostUser, detectiveUser1, detectiveUser2)) {
            int position = gameState.getPlayerPosition(user.id());
            boolean isValidStartPosition = false;
            for (int validPos : validStartPositions) {
                if (position == validPos) {
                    isValidStartPosition = true;
                    break;
                }
            }
            assertTrue(isValidStartPosition);
        }
    }

    @Test
    void testPlayerPositionsAfterInitialization() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        for (User user : List.of(hostUser, detectiveUser1)) {
            assertNotNull(gameState.getPlayerPosition(user.id()));
        }
    }

    @Test
    void testDifferentStartPositionsForDifferentPlayers() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        Integer pos1 = gameState.getPlayerPosition(hostUser.id());
        Integer pos2 = gameState.getPlayerPosition(detectiveUser1.id());

        assertNotNull(pos1);
        assertNotNull(pos2);

        assertTrue(pos1 >= 1 && pos1 <= 199);
        assertTrue(pos2 >= 1 && pos2 <= 199);
    }
}