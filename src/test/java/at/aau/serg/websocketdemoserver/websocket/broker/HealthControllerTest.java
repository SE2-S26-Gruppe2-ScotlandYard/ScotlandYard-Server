package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import at.aau.serg.websocketdemoserver.service.SessionAuthService;
import at.aau.serg.websocketdemoserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private GameController gameController;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private UserService userService;

    @Mock
    private SessionAuthService sessionAuthService;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        lenient().when(gameController.getActiveGamesCount()).thenReturn(0);
        lenient().when(lobbyService.getActiveLobbies()).thenReturn(Map.of());
        lenient().when(userService.getActiveUserCount()).thenReturn(0);
    }

    @Test
    void testHealth_isCalled_returnsStatusOk() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testHealth_isCalled_bodyIsNotNull() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertNotNull(response.getBody());
    }

    @Test
    void testHealth_isCalled_statusIsUp() {
        Map<String, Object> body = healthController.health().getBody();

        assertEquals("UP", body.get("status"));
    }

    @Test
    void testHealth_isCalled_timestampIsPresent() {
        Map<String, Object> body = healthController.health().getBody();

        assertNotNull(body.get("timestamp"));
        assertFalse(body.get("timestamp").toString().isBlank());
    }

    @Test
    void testHealth_whenNoActiveGames_activeGamesIsZero() {
        Map<String, Object> body = healthController.health().getBody();

        assertEquals(0, body.get("activeGames"));
    }

    @Test
    void testHealth_whenMultipleActiveGames_returnsCorrectCount() {
        when(gameController.getActiveGamesCount()).thenReturn(3);

        Map<String, Object> body = healthController.health().getBody();

        assertEquals(3, body.get("activeGames"));
    }

    @Test
    void testHealth_whenNoActiveLobbies_activeLobbiesIsZero() {
        Map<String, Object> body = healthController.health().getBody();

        assertEquals(0, body.get("activeLobbies"));
    }

    @Test
    void testHealth_whenMultipleActiveLobbies_returnsCorrectCount() {
        when(lobbyService.getActiveLobbies()).thenReturn(Map.of("l1", mock(Lobby.class), "l2", mock(Lobby.class), "l3", mock(Lobby.class)));

        Map<String, Object> body = healthController.health().getBody();

        assertEquals(3, body.get("activeLobbies"));
    }

    @Test
    void testHealth_whenNoUsers_activeUsersIsZero() {
        Map<String, Object> body = healthController.health().getBody();

        assertEquals(0, body.get("activeUsers"));
    }

    @Test
    void testHealth_whenMultipleActiveUsers_returnsCorrectCount() {
        when(userService.getActiveUserCount()).thenReturn(3);

        Map<String, Object> body = healthController.health().getBody();

        assertEquals(3, body.get("activeUsers"));
    }

    @Test
    void testUsers_returnsStatusOk() {
        when(userService.getAllUsers()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = healthController.users();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUsers_whenNoUsers_returnsEmptyListAndZeroCount() {
        when(userService.getAllUsers()).thenReturn(List.of());

        Map<String, Object> body = healthController.users().getBody();

        assertEquals(0, body.get("count"));
        assertTrue(((List<?>) body.get("users")).isEmpty());
    }

    @Test
    void testUsers_includesIdNicknameAndConnectedStatus() {
        User connectedUser = new User("u1", "Alice");
        User disconnectedUser = new User("u2", "Bob");
        when(userService.getAllUsers()).thenReturn(List.of(connectedUser, disconnectedUser));
        when(sessionAuthService.isUserDisconnected("u1")).thenReturn(false);
        when(sessionAuthService.isUserDisconnected("u2")).thenReturn(true);

        Map<String, Object> body = healthController.users().getBody();
        List<?> users = (List<?>) body.get("users");

        assertEquals(2, body.get("count"));
        assertEquals(2, users.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> alice = (Map<String, Object>) users.get(0);
        assertEquals("u1", alice.get("id"));
        assertEquals("Alice", alice.get("nickName"));
        assertEquals(true, alice.get("connected"));

        @SuppressWarnings("unchecked")
        Map<String, Object> bob = (Map<String, Object>) users.get(1);
        assertEquals("u2", bob.get("id"));
        assertEquals("Bob", bob.get("nickName"));
        assertEquals(false, bob.get("connected"));
    }

    @Test
    void testLobbies_returnsStatusOk() {
        when(lobbyService.getActiveLobbies()).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response = healthController.lobbies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testLobbies_whenNoLobbies_returnsEmptyListAndZeroCount() {
        when(lobbyService.getActiveLobbies()).thenReturn(Map.of());

        Map<String, Object> body = healthController.lobbies().getBody();

        assertEquals(0, body.get("count"));
        assertTrue(((List<?>) body.get("lobbies")).isEmpty());
    }

    @Test
    void testLobbies_includesLobbyAndPlayerDetails() {
        User host = new User("host1", "Host");
        Lobby mockLobby = mock(Lobby.class);
        when(mockLobby.getId()).thenReturn("ABCDE");
        when(mockLobby.getName()).thenReturn("Host's Lobby");
        when(mockLobby.getHostId()).thenReturn("host1");
        when(mockLobby.isStarted()).thenReturn(false);
        when(mockLobby.isLocked()).thenReturn(false);
        when(mockLobby.getUsers()).thenReturn(List.of(host));
        when(mockLobby.getReadyStatus()).thenReturn(Map.of("host1", true));
        when(mockLobby.getSelectedRoles()).thenReturn(Map.of("host1", Role.MRX));
        when(lobbyService.getActiveLobbies()).thenReturn(Map.of("ABCDE", mockLobby));
        when(sessionAuthService.isUserDisconnected("host1")).thenReturn(false);

        Map<String, Object> body = healthController.lobbies().getBody();
        List<?> lobbies = (List<?>) body.get("lobbies");

        assertEquals(1, body.get("count"));
        @SuppressWarnings("unchecked")
        Map<String, Object> lobbySummary = (Map<String, Object>) lobbies.get(0);
        assertEquals("ABCDE", lobbySummary.get("id"));
        assertEquals("Host's Lobby", lobbySummary.get("name"));
        assertEquals("host1", lobbySummary.get("hostId"));
        assertEquals(false, lobbySummary.get("started"));
        assertEquals(false, lobbySummary.get("locked"));
        assertEquals(1, lobbySummary.get("playerCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) lobbySummary.get("players");
        assertEquals(1, players.size());
        assertEquals("host1", players.get(0).get("id"));
        assertEquals("Host", players.get(0).get("nickName"));
        assertEquals(true, players.get(0).get("ready"));
        assertEquals(Role.MRX, players.get(0).get("role"));
        assertEquals(true, players.get(0).get("connected"));
    }

    @Test
    void testGames_returnsStatusOk() {
        when(gameController.getActiveGames()).thenReturn(Map.of());

        ResponseEntity<Map<String, Object>> response = healthController.games();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGames_whenNoGames_returnsEmptyListAndZeroCount() {
        when(gameController.getActiveGames()).thenReturn(Map.of());

        Map<String, Object> body = healthController.games().getBody();

        assertEquals(0, body.get("count"));
        assertTrue(((List<?>) body.get("games")).isEmpty());
    }

    @Test
    void testGames_includesGameSummaryFields() {
        GameState mockGame = mock(GameState.class);
        when(mockGame.getGameId()).thenReturn("game1");
        when(mockGame.getHostId()).thenReturn("host1");
        when(mockGame.getCurrentRound()).thenReturn(2);
        when(mockGame.getCurrentPhase()).thenReturn(TurnType.MRX);
        when(mockGame.getPlayerNames()).thenReturn(Map.of("host1", "Host", "det1", "Detective1"));
        when(mockGame.allPlayersHaveStartPosition()).thenReturn(true);
        when(gameController.getActiveGames()).thenReturn(Map.of("game1", mockGame));
        when(sessionAuthService.isUserDisconnected("host1")).thenReturn(false);
        when(sessionAuthService.isUserDisconnected("det1")).thenReturn(true);

        Map<String, Object> body = healthController.games().getBody();
        List<?> games = (List<?>) body.get("games");

        assertEquals(1, body.get("count"));
        @SuppressWarnings("unchecked")
        Map<String, Object> gameSummary = (Map<String, Object>) games.get(0);
        assertEquals("game1", gameSummary.get("gameId"));
        assertEquals("host1", gameSummary.get("hostId"));
        assertEquals(2, gameSummary.get("currentRound"));
        assertEquals(TurnType.MRX, gameSummary.get("currentPhase"));
        assertEquals(2, gameSummary.get("playerCount"));
        assertEquals(true, gameSummary.get("allPlayersHaveStartPosition"));

        @SuppressWarnings("unchecked")
        List<String> disconnected = (List<String>) gameSummary.get("disconnectedPlayers");
        assertEquals(List.of("det1"), disconnected);
    }

    @Test
    void testGames_neverExposesBoardPositionsOrTickets() {
        GameState mockGame = mock(GameState.class);
        when(mockGame.getGameId()).thenReturn("game1");
        when(mockGame.getHostId()).thenReturn("host1");
        when(mockGame.getCurrentRound()).thenReturn(1);
        when(mockGame.getCurrentPhase()).thenReturn(TurnType.MRX);
        when(mockGame.getPlayerNames()).thenReturn(Map.of("host1", "Host"));
        when(mockGame.allPlayersHaveStartPosition()).thenReturn(false);
        when(gameController.getActiveGames()).thenReturn(Map.of("game1", mockGame));
        when(sessionAuthService.isUserDisconnected("host1")).thenReturn(false);

        Map<String, Object> body = healthController.games().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> gameSummary = (Map<String, Object>) ((List<?>) body.get("games")).get(0);

        assertFalse(gameSummary.containsKey("mrXPosition"));
        assertFalse(gameSummary.containsKey("detectivePositions"));
        assertFalse(gameSummary.containsKey("playerTickets"));
        assertFalse(gameSummary.containsKey("mrXMoveHistory"));
    }
}