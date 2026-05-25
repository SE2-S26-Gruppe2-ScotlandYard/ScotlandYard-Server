package at.aau.serg.websocketdemoserver.gamelogic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
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

        hostUser = new User("user1", "HostPlayer");
        detectiveUser1 = new User("user2", "Detective1");
        detectiveUser2 = new User("user3", "Detective2");
        mrXUser = new User("user4", "MrXPlayer");

        mockLobby = mock(Lobby.class);
    }

    @Test
    void testConstructor() {
        assertNotNull(gameState);
        assertEquals("game123", gameState.getGameId());
        assertNotNull(gameState.getBoard());
    }

    @Test
    void testInitializePlayersFromLobbySuccess() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

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
        gameState.initializePlayersFromLobby(mockLobby);

        // positions are assigned on demand, not automatically
        int pos1 = gameState.assignStartPosition(hostUser.id());
        int pos2 = gameState.assignStartPosition(detectiveUser1.id());
        int pos3 = gameState.assignStartPosition(detectiveUser2.id());
        int pos4 = gameState.assignStartPosition(mrXUser.id());

        assertTrue(pos1 >= 1 && pos1 <= 199);
        assertTrue(pos2 >= 1 && pos2 <= 199);
        assertTrue(pos3 >= 1 && pos3 <= 199);
        assertTrue(pos4 >= 1 && pos4 <= 199);
    }

    @Test
    void testSetPlayerPositionValid() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 42);

        assertEquals(42, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testSetPlayerPositionInvalidBelowRange() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String playerId = hostUser.id();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gameState.setPlayerPosition(playerId, 0)
        );

        assertEquals("Position must be between 1 and 199", exception.getMessage());
    }

    @Test
    void testSetPlayerPositionInvalidAboveRange() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String playerId = hostUser.id();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> gameState.setPlayerPosition(playerId, 200)
        );

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
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.assignStartPosition(mrXUser.id());
        gameState.assignStartPosition(detectiveUser1.id());

        Integer mrXPosition = gameState.getMrXPosition();

        assertNotNull(mrXPosition);
        assertTrue(mrXPosition >= 1 && mrXPosition <= 199);
    }

    @Test
    void testGetMrXPositionNoMrX() {
        setupDetectiveLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        Integer mrXPosition = gameState.getMrXPosition();

        assertNull(mrXPosition);
    }

    @Test
    void testMultiplePlayersSamePosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 100);
        gameState.setPlayerPosition(detectiveUser1.id(), 100);

        assertEquals(100, gameState.getPlayerPosition(hostUser.id()));
        assertEquals(100, gameState.getPlayerPosition(detectiveUser1.id()));
    }

    @Test
    void testUpdatePlayerPosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 10);
        assertEquals(10, gameState.getPlayerPosition(hostUser.id()));

        gameState.setPlayerPosition(hostUser.id(), 50);
        assertEquals(50, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testStartPositionsAreWithinFullBoardRange() {
        setupDetectiveLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        for (User user : List.of(hostUser, detectiveUser1, detectiveUser2)) {
            int position = gameState.assignStartPosition(user.id());
            assertTrue(position >= 1 && position <= 199);
        }
    }

    @Test
    void testPlayerPositionsAfterInitialization() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        for (User user : List.of(hostUser, detectiveUser1)) {
            int pos = gameState.assignStartPosition(user.id());
            assertTrue(pos >= 1 && pos <= 199);
        }
    }

    @Test
    void testDifferentStartPositionsForDifferentPlayers() {
        setupDetectiveLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        Integer pos1 = gameState.assignStartPosition(hostUser.id());
        Integer pos2 = gameState.assignStartPosition(detectiveUser1.id());

        assertNotNull(pos1);
        assertNotNull(pos2);

        assertTrue(pos1 >= 1 && pos1 <= 199);
        assertTrue(pos2 >= 1 && pos2 <= 199);
        assertNotEquals(pos1, pos2);
    }

    // getDetectivePositions

    @Test
    void testGetDetectivePositionsExcludesMrX() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertNotNull(detectivePositions);
        assertEquals(2, detectivePositions.size());
        assertFalse(detectivePositions.containsKey(mrXUser.id()));
        assertTrue(detectivePositions.containsKey(hostUser.id()));
        assertTrue(detectivePositions.containsKey(detectiveUser1.id()));
    }

    @Test
    void testGetDetectivePositionsNoDetectives() {
        setupOnlyMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertNotNull(detectivePositions);
        assertTrue(detectivePositions.isEmpty());
    }

    @Test
    void testGetDetectivePositionsReturnsUnmodifiableMap() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertThrows(UnsupportedOperationException.class, () -> {
            detectivePositions.put("newPlayer", 100);
        });
    }

    @Test
    void testGetDetectivePositionsUpdates() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // update detective position
        gameState.setPlayerPosition(hostUser.id(), 150);
        Map<String, Integer> detectivePositions = gameState.getDetectivePositions();

        assertEquals(150, detectivePositions.get(hostUser.id()));
    }

    @Test
    void testGetDetectivePositionsAllDetectives() {
        setupDetectiveLobby();
        gameState.initializePlayersFromLobby(mockLobby);

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
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        boolean result = gameState.movePlayer("nonExistentPlayer", TicketType.WALKING, 50);

        assertFalse(result);
    }

    @Test
    void testMovePlayerPlayerHasNoPosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // remove position
        gameState.playerPositions.remove(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 50);

        assertFalse(result);
    }

    @Test
    void testMovePlayerInvalidMoveNoTicket() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        gameState.assignStartPosition(hostUser.id());

        int currentPos = gameState.getPlayerPosition(hostUser.id());

        // try to move with a ticket that detective doesn't have
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.BLACK, currentPos + 1);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerInvalidMoveNoConnection() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 1);

        // try to move to a station that is not connected
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 199);

        assertFalse(result);
        assertEquals(1, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerDeductsTicket() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // set to a known position with connections
        gameState.setPlayerPosition(hostUser.id(), 2);

        // get ticket count before move
        int ticketCountBefore = gameState.getPlayer(hostUser.id()).getTickets().get(TicketType.WALKING);

        // set TurnType to DETECTIVES
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);

        assertTrue(result);
        int ticketCountAfter = gameState.getPlayer(hostUser.id()).getTickets().get(TicketType.WALKING);
        assertEquals(ticketCountBefore - 1, ticketCountAfter);
    }

    @Test
    void testMovePlayerInsufficientTickets() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // set to a known position with connections
        gameState.setPlayerPosition(hostUser.id(), 2);

        // use all walking tickets (Detective starts with 10 walking tickets)
        for (int i = 0; i < 6; i++) {
            // set TurnType to DETECTIVES
            gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
            gameState.getRoundController().addPendingDetectives(hostUser.id());

            gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);

            // set TurnType to DETECTIVES
            gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
            gameState.getRoundController().addPendingDetectives(hostUser.id());

            // move back to original position for next move
            gameState.movePlayer(hostUser.id(), TicketType.WALKING, 2);
        }

        // try to move when no tickets left
        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, 20);

        assertFalse(result);
    }

    @Test
    void testMrXMoveWithBlackTicket() {
        setupOnlyMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

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
        setupOnlyMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // set to a known position
        gameState.setPlayerPosition(mrXUser.id(), 2);

        // Mr. X should have unlimited walking tickets
        for (int i = 0; i < 20; i++) {
            boolean result = gameState.movePlayer(mrXUser.id(), TicketType.WALKING, 20);
            assertTrue(result);
            assertEquals(20, gameState.getPlayerPosition(mrXUser.id()));
            // set TurnType to MRX again
            gameState.getRoundController().setCurrentPhase(TurnType.MRX);
            // move back
            gameState.movePlayer(mrXUser.id(), TicketType.WALKING, 2);
            // set TurnType to MRX again
            gameState.getRoundController().setCurrentPhase(TurnType.MRX);
        }
    }

    @Test
    void testMovePlayerSameStation() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        gameState.assignStartPosition(hostUser.id());

        int currentPos = gameState.getPlayerPosition(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.WALKING, currentPos);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerNegativePosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.movePlayer(hostUser.id(), TicketType.WALKING, -5);

        assertNotEquals(-5, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerPositionOutOfRange() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.movePlayer(hostUser.id(), TicketType.WALKING, 300);

        assertNotEquals(300, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testDetectiveCannotUseBlackTicket() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 108);
        int currentPos = gameState.getPlayerPosition(hostUser.id());

        boolean result = gameState.movePlayer(hostUser.id(), TicketType.BLACK, 115);

        assertFalse(result);
        assertEquals(currentPos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testMovePlayerWithDifferentTicketTypes() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.setPlayerPosition(hostUser.id(), 77);

        // set TurnType to DETECTIVES
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(hostUser.id());

        // try different ticket types that should be valid
        gameState.movePlayer(hostUser.id(), TicketType.WALKING, 78);
        assertEquals(78, gameState.getPlayerPosition(hostUser.id()));

        // set TurnType to DETECTIVES
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(hostUser.id());

        gameState.movePlayer(hostUser.id(), TicketType.ESCOOTER, 79);
        assertEquals(79, gameState.getPlayerPosition(hostUser.id()));

        // set TurnType to DETECTIVES
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(hostUser.id());

        gameState.movePlayer(hostUser.id(), TicketType.CARSHARING, 111);
        assertEquals(111, gameState.getPlayerPosition(hostUser.id()));
    }

    // isCaught
    @Test
    void testMrXCollisionWithDetective() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // set same position for Mr.X and a detective
        gameState.setPlayerPosition(mrXUser.id(), 42);
        gameState.setPlayerPosition(hostUser.id(), 42);

        assertTrue(gameState.isCaught());
    }

    @Test
    void testMrXNoCollision() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // set different positions
        gameState.setPlayerPosition(mrXUser.id(), 10);
        gameState.setPlayerPosition(hostUser.id(), 20);

        assertFalse(gameState.isCaught());
    }

    @Test
    void testGetCurrentRound() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int currentRound = gameState.getCurrentRound();
        assertTrue(currentRound >= 0);
    }

    // checkGameResult
    @Test
    void checkGameOver_ongoing() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        gameState.setPlayerPosition(mrXUser.id(), 10);
        gameState.setPlayerPosition(hostUser.id(), 20);
        gameState.setPlayerPosition(detectiveUser1.id(), 30);

        assertEquals(GameResult.ONGOING, gameState.checkGameResult());
    }

    @Test
    void checkGameOver_mrxCaught() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        gameState.setPlayerPosition(mrXUser.id(), 42);
        gameState.setPlayerPosition(hostUser.id(), 42);      // same field!

        assertEquals(GameResult.DETECTIVES_WIN, gameState.checkGameResult());
    }

    @Test
    void checkGameOver_detectivesWinLastRound() {
        // past MAX_ROUNDS, if MrX is caught, DETECTIVES_WIN
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        while (gameState.getCurrentRound() <= GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }

        gameState.setPlayerPosition(mrXUser.id(), 42);
        gameState.setPlayerPosition(hostUser.id(), 42);

        assertEquals(GameResult.DETECTIVES_WIN, gameState.checkGameResult());
    }

    @Test
    void checkGameOver_pastMaxRoundsNotCaught() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        while (gameState.getCurrentRound() <= GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        gameState.setPlayerPosition(mrXUser.id(), 10);
        gameState.setPlayerPosition(hostUser.id(), 20);
        gameState.setPlayerPosition(detectiveUser1.id(), 30);

        assertEquals(GameResult.MRX_WINS, gameState.checkGameResult());
    }

    @Test
    void checkGameOver_atMaxRoundsExactly() {
        setupBasicMrxLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        while (gameState.getCurrentRound() < GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        assertEquals(GameState.MAX_ROUNDS, gameState.getCurrentRound());

        gameState.setPlayerPosition(mrXUser.id(), 10);
        gameState.setPlayerPosition(hostUser.id(), 20);
        gameState.setPlayerPosition(detectiveUser1.id(), 30);

        assertEquals(GameResult.ONGOING, gameState.checkGameResult());
    }

    // supporting methods
    private void setupBasicLobby() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
    }

    private void setupBasicMrxLobby() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, mrXUser));
        when(mockLobby.getSelectedRole(hostUser.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(detectiveUser1.id())).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);
    }

    private void setupOnlyMrxLobby() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser));
        when(mockLobby.getSelectedRole(mrXUser.id())).thenReturn(Role.MRX);
    }

    private void setupDetectiveLobby() {
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(hostUser, detectiveUser1, detectiveUser2));
        when(mockLobby.getSelectedRole(anyString())).thenReturn(Role.DETECTIVE);
    }

    @Test
    void testStartPositionsAreUnique() {
        setupDetectiveLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos1 = gameState.assignStartPosition(hostUser.id());
        int pos2 = gameState.assignStartPosition(detectiveUser1.id());
        int pos3 = gameState.assignStartPosition(detectiveUser2.id());

        assertNotEquals(pos1, pos2);
        assertNotEquals(pos1, pos3);
        assertNotEquals(pos2, pos3);
    }

    // assignStartPosition tests
    @Test
    void testAssignStartPosition_inRange() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos = gameState.assignStartPosition(hostUser.id());

        assertTrue(pos >= 1 && pos <= 199);
    }

    @Test
    void testAssignStartPosition_samePlayerGetsSamePosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos1 = gameState.assignStartPosition(hostUser.id());
        int pos2 = gameState.assignStartPosition(hostUser.id());

        assertEquals(pos1, pos2);
    }

    @Test
    void testAssignStartPosition_twoPlayersGetDifferentPositions() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos1 = gameState.assignStartPosition(hostUser.id());
        int pos2 = gameState.assignStartPosition(detectiveUser1.id());

        assertNotEquals(pos1, pos2);
    }

    @Test
    void testAssignStartPosition_unknownPlayerThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gameState.assignStartPosition("unknownPlayer"));
    }

    // ── assignStartPosition(playerId, selectedPosition) – cheat feature ───

    @Test
    void testAssignStartPosition_withValidSelectedPosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos = gameState.assignStartPosition(hostUser.id(), 42);

        assertEquals(42, pos);
        assertEquals(42, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testAssignStartPosition_selectedNull_fallsBackToRandom() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos = gameState.assignStartPosition(hostUser.id(), null);

        assertTrue(pos >= 1 && pos <= 199);
        assertEquals(pos, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testAssignStartPosition_selectedBelowRange_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String playerId = hostUser.id();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameState.assignStartPosition(playerId, 0));
        assertTrue(ex.getMessage().contains("1 and 199"));
    }

    @Test
    void testAssignStartPosition_selectedAboveRange_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String playerId = hostUser.id();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameState.assignStartPosition(playerId, 200));
        assertTrue(ex.getMessage().contains("1 and 199"));
    }

    @Test
    void testAssignStartPosition_selectedNegative_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String playerId = hostUser.id();

        assertThrows(IllegalArgumentException.class,
                () -> gameState.assignStartPosition(playerId, -5));
    }

    @Test
    void testAssignStartPosition_selectedAlreadyTaken_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // first player takes position 50
        gameState.assignStartPosition(hostUser.id(), 50);

        // second player tries the same position
        String secondId = detectiveUser1.id();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> gameState.assignStartPosition(secondId, 50));
        assertTrue(ex.getMessage().contains("already taken"));
    }

    @Test
    void testAssignStartPosition_selectedPositionReturnsExistingIfAlreadyAssigned() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        // assign once
        gameState.assignStartPosition(hostUser.id(), 77);

        // calling again (even with a different selection) returns the already-assigned position
        int pos = gameState.assignStartPosition(hostUser.id(), 33);

        assertEquals(77, pos);
    }

    @Test
    void testAssignStartPosition_unknownPlayerWithSelectedPosition_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gameState.assignStartPosition("unknownPlayer", 42));
    }

    @Test
    void testAssignStartPosition_twoDifferentSelectedPositions_areUnique() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.assignStartPosition(hostUser.id(), 10);
        gameState.assignStartPosition(detectiveUser1.id(), 20);

        assertEquals(10, gameState.getPlayerPosition(hostUser.id()));
        assertEquals(20, gameState.getPlayerPosition(detectiveUser1.id()));
    }

    // ── initializePlayersFromLobby ─────────────────────────────────────────

    @Test
    void testInitializePlayersFromLobby_populatesPlayers() {
        // Uses real Lobby objects (no mock) — no canStartGame() check required
        at.aau.serg.websocketdemoserver.lobby.Lobby lobby =
                new at.aau.serg.websocketdemoserver.lobby.Lobby("TestLobby", hostUser);
        lobby.addUser(detectiveUser1);
        lobby.selectRole(hostUser.id(), Role.MRX);
        lobby.selectRole(detectiveUser1.id(), Role.DETECTIVE);

        gameState.initializePlayersFromLobby(lobby);

        assertNotNull(gameState.getPlayer(hostUser.id()));
        assertNotNull(gameState.getPlayer(detectiveUser1.id()));
        assertTrue(gameState.getPlayer(hostUser.id()).isMrX());
        assertFalse(gameState.getPlayer(detectiveUser1.id()).isMrX());
    }

    @Test
    void testInitializePlayersFromLobby_allowsFewerThanMinPlayers() {
        // Only 1 player — initializePlayersFromLobby must NOT throw (no canStartGame check)
        at.aau.serg.websocketdemoserver.lobby.Lobby lobby =
                new at.aau.serg.websocketdemoserver.lobby.Lobby("Solo", hostUser);
        lobby.selectRole(hostUser.id(), Role.MRX);

        assertDoesNotThrow(() -> gameState.initializePlayersFromLobby(lobby));
        assertNotNull(gameState.getPlayer(hostUser.id()));
    }

    @Test
    void testInitializePlayersFromLobby_assignStartPositionWorksAfter() {
        at.aau.serg.websocketdemoserver.lobby.Lobby lobby =
                new at.aau.serg.websocketdemoserver.lobby.Lobby("TestLobby", hostUser);
        lobby.addUser(detectiveUser1);
        lobby.selectRole(hostUser.id(), Role.MRX);
        lobby.selectRole(detectiveUser1.id(), Role.DETECTIVE);

        gameState.initializePlayersFromLobby(lobby);

        int pos = gameState.assignStartPosition(hostUser.id());
        assertTrue(pos >= 1 && pos <= 199);
    }

    // ── confirmStartPosition ──────────────────────────────────────────────────

    @Test
    void testConfirmStartPosition_validPosition_setsAndReturnsIt() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        int pos = gameState.confirmStartPosition(hostUser.id(), 42);

        assertEquals(42, pos);
        assertEquals(42, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testConfirmStartPosition_belowRange_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String pid = hostUser.id();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameState.confirmStartPosition(pid, 0));
        assertTrue(ex.getMessage().contains("1 and 199"));
    }

    @Test
    void testConfirmStartPosition_aboveRange_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String pid = hostUser.id();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameState.confirmStartPosition(pid, 200));
        assertTrue(ex.getMessage().contains("1 and 199"));
    }

    @Test
    void testConfirmStartPosition_negativeValue_throwsException() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        String pid = hostUser.id();

        assertThrows(IllegalArgumentException.class,
                () -> gameState.confirmStartPosition(pid, -1));
    }

    @Test
    void testConfirmStartPosition_positionAlreadyTakenByOtherPlayer_assignsFreePosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.confirmStartPosition(hostUser.id(), 50);

        // second player requests the same position – server must silently pick a free one
        String secondId = detectiveUser1.id();
        int fallback = gameState.confirmStartPosition(secondId, 50);

        // fallback must be valid and different from the taken position
        assertTrue(fallback >= 1 && fallback <= 199);
        assertNotEquals(50, fallback);
        // both players must have distinct positions
        assertNotEquals(gameState.getPlayerPosition(hostUser.id()),
                gameState.getPlayerPosition(secondId));
    }

    @Test
    void testConfirmStartPosition_samePlayerCanUpdatePosition() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.confirmStartPosition(hostUser.id(), 30);
        int updated = gameState.confirmStartPosition(hostUser.id(), 80);

        assertEquals(80, updated);
        assertEquals(80, gameState.getPlayerPosition(hostUser.id()));
    }

    @Test
    void testConfirmStartPosition_unknownPlayer_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> gameState.confirmStartPosition("ghost", 42));
    }

    // ── allPlayersHaveStartPosition ───────────────────────────────────────────

    @Test
    void testAllPlayersHaveStartPosition_falseWhenNoneSet() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        assertFalse(gameState.allPlayersHaveStartPosition());
    }

    @Test
    void testAllPlayersHaveStartPosition_falseWhenPartiallySet() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.confirmStartPosition(hostUser.id(), 10);
        // detectiveUser1 not yet confirmed

        assertFalse(gameState.allPlayersHaveStartPosition());
    }

    @Test
    void testAllPlayersHaveStartPosition_trueWhenAllSet() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);

        gameState.confirmStartPosition(hostUser.id(), 10);
        gameState.confirmStartPosition(detectiveUser1.id(), 20);

        assertTrue(gameState.allPlayersHaveStartPosition());
    }

    @Test
    void testAllPlayersHaveStartPosition_falseWhenGameHasNoPlayers() {
        // fresh GameState with no players at all must return false
        assertFalse(new GameState("empty-game").allPlayersHaveStartPosition());
    }

    // ── boundary values for assignStartPosition(playerId, selected) ──────────

    @Test
    void testAssignStartPosition_selectedBoundaryLow_valid() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        int pos = gameState.assignStartPosition(hostUser.id(), 1);
        assertEquals(1, pos);
    }

    @Test
    void testAssignStartPosition_selectedBoundaryHigh_valid() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        int pos = gameState.assignStartPosition(hostUser.id(), 199);
        assertEquals(199, pos);
    }

    // ── boundary values for confirmStartPosition ──────────────────────────────

    @Test
    void testConfirmStartPosition_boundaryLow_valid() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        int pos = gameState.confirmStartPosition(hostUser.id(), 1);
        assertEquals(1, pos);
    }

    @Test
    void testConfirmStartPosition_boundaryHigh_valid() {
        setupBasicLobby();
        gameState.initializePlayersFromLobby(mockLobby);
        int pos = gameState.confirmStartPosition(hostUser.id(), 199);
        assertEquals(199, pos);
    }
}