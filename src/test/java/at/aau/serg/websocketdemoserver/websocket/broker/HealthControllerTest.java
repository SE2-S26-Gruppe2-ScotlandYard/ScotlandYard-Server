package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import at.aau.serg.websocketdemoserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        when(gameController.getActiveGamesCount()).thenReturn(0);
        when(lobbyService.getActiveLobbies()).thenReturn(Map.of());
        when(userService.getActiveUserCount()).thenReturn(0);
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
}