package at.aau.serg.websocketdemoserver.gamelogic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
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


    // TODO: refactor (too much boilerplate code)

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

    // getDetectivePositions

    @Test
    void testGetDetectivePositionsExcludesMrX() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, mrXUser));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertNotNull(detectivePositions);
        assertEquals(2, detectivePositions.size());
        assertFalse(detectivePositions.containsKey(mrXUser.id()));
        assertTrue(detectivePositions.containsKey(hostUser.id()));
        assertTrue(detectivePositions.containsKey(detectiveUser1.id()));
    }

    @Test
    void testGetDetectivePositionsNoDetectives() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser));
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertNotNull(detectivePositions);
        assertTrue(detectivePositions.isEmpty());
    }

    @Test
    void testGetDetectivePositionsReturnsUnmodifiableMap() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, mrXUser));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertThrows(UnsupportedOperationException.class, () -> {
            detectivePositions.put("newPlayer", 100);
        });
    }

    @Test
    void testGetDetectivePositionsUpdates() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, mrXUser));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        // update detective position
        gameState.setPlayerPosition(hostUser.id(), 150);
        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertEquals(150, detectivePositions.get(hostUser.id()));
    }

    @Test
    void testGetDetectivePositionsAllDetectives() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        // set specific positions
        gameState.setPlayerPosition(hostUser.id(), 10);
        gameState.setPlayerPosition(detectiveUser1.id(), 20);
        gameState.setPlayerPosition(detectiveUser2.id(), 30);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertEquals(3, detectivePositions.size());
        assertEquals(10, detectivePositions.get(hostUser.id()));
        assertEquals(20, detectivePositions.get(detectiveUser1.id()));
        assertEquals(30, detectivePositions.get(detectiveUser2.id()));
    }

    // movePlayer

    @Test
    void testMovePlayerPlayerDoesNotExist() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        boolean result = gameState.movePlayer("nonExistentPlayer", TicketType.WALKING, 50);

        assertFalse(result);
    }

    @Test
    void testMovePlayerPlayerHasNoPosition() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        // remove position
        gameState.playerPositions.remove(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 50);

        assertFalse(result);
    }

    @Test
    void testMovePlayerInvalidMoveNoTicket() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        int currentPos = gameState.getPlayerPosition(hostUser.id());

        // try to move with a ticket that detective doesn't have
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.BLACK, currentPos + 1);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerInvalidMoveNoConnection() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 1);

        // try to move to a station that is not connected
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 199);

        assertFalse(result);
        assertEquals(1, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerDeductsTicket() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        // set to a known position with connections
        gameState.setPlayerPosition(hostUser.id(), 2);

        // get ticket count before move
        int ticketCountBefore = gameState.getPlayer(hostUser.id()).getTickets().get(TicketType.WALKING);

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);

        assertTrue(result);
        int ticketCountAfter = gameState.getPlayer(hostUser.id()).getTickets().get(TicketType.WALKING);
        assertEquals(ticketCountBefore - 1, ticketCountAfter);
    }

    @Test
    void testMovePlayerInsufficientTickets() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        // set to a known position with connections
        gameState.setPlayerPosition(hostUser.id(), 2);

        // use all walking tickets (Detective starts with 10 walking tickets)
        for (int i = 0; i < 6; i++) {
            gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);
            // move back to original position for next move
            gameState.movePlayer(hostUser.id(), TicketType.WALKING, 2);
        }

        // try to move when no tickets left
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);

        assertFalse(result);
    }

    @Test
    void testMrXMoveWithBlackTicket() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser));
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        // set to a known position
        gameState.setPlayerPosition(mrXUser.id(), 108);

        int blackTicketsBefore = gameState.getPlayer(mrXUser.id()).getTickets().get(TicketType.BLACK);

        // move using BLACK ticket
        boolean result = gameState.movePlayer(mrXUser.id(), TicketType.BLACK, 115);

        assertTrue(result);
        int blackTicketsAfter = gameState.getPlayer(mrXUser.id()).getTickets().get(TicketType.BLACK);
        assertEquals(blackTicketsBefore - 1, blackTicketsAfter);
        assertEquals(115, gameState.getPlayerPosition(mrXUser.id()));
    }

    @Test
    void testMrXMoveUnlimitedRegularTickets() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser));
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);

        gameState.initializeFromLobby(mockLobby);

        // set to a known position
        gameState.setPlayerPosition(mrXUser.id(), 2);

        // Mr. X should have unlimited walking tickets
        for (int i = 0; i < 20; i++) {
            boolean result = gameState.movePlayer(mrXUser.id(), TicketType.WALKING, 20);
            assertTrue(result);
            assertEquals(20, gameState.getPlayerPosition(mrXUser.id()));
            // move back
            gameState.movePlayer(mrXUser.id(), TicketType.WALKING, 2);
        }
    }

    @Test
    void testMovePlayerSameStation() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        int currentPos = gameState.getPlayerPosition(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, currentPos);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerNegativePosition() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.movePlayer(hostUser.id(), TicketType.WALKING, -5);

        assertNotEquals(-5, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerPositionOutOfRange() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.movePlayer(hostUser.id(), TicketType.WALKING, 300);

        assertNotEquals(300, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testDetectiveCannotUseBlackTicket() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 108);
        int currentPos = gameState.getPlayerPosition(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.BLACK, 115);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerWithDifferentTicketTypes() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);

        gameState.initializeFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 77);

        // try different ticket types that should be valid
        gameState.movePlayer(hostUser.id(), TicketType.WALKING, 78);
        assertEquals(78, gameState.getPlayerPosition(hostUser.id()));

        gameState.movePlayer(hostUser.id(), TicketType.ESCOOTER, 79);
        assertEquals(79, gameState.getPlayerPosition(hostUser.id()));

        gameState.movePlayer(hostUser.id(), TicketType.CARSHARING, 111);
        assertEquals(111, gameState.getPlayerPosition(hostUser.id()));
    }

}