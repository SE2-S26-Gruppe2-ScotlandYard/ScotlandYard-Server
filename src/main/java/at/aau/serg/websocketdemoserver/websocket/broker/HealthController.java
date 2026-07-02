package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import at.aau.serg.websocketdemoserver.service.SessionAuthService;
import at.aau.serg.websocketdemoserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP health/monitoring endpoints. Intended for internal/ops use, not authenticated.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    private final GameController gameController;
    private final LobbyService lobbyService;
    private final UserService userService;
    private final SessionAuthService sessionAuthService;

    @Autowired
    public HealthController(GameController gameController, LobbyService lobbyService, UserService userService, SessionAuthService sessionAuthService
    ) {
        this.gameController = gameController;
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.sessionAuthService = sessionAuthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        body.put("activeUsers", userService.getActiveUserCount());
        body.put("activeLobbies", lobbyService.getActiveLobbies().size());
        body.put("activeGames", gameController.getActiveGamesCount());
        return ResponseEntity.ok(body);
    }

    /**
     * Lists all currently registered users with their connection status.
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users() {
        List<Map<String, Object>> userList = userService.getAllUsers().stream()
                .map(this::toUserSummary)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", userList.size());
        body.put("users", userList);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toUserSummary(User user) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", user.id());
        summary.put("nickName", user.nickName());
        summary.put("connected", !sessionAuthService.isUserDisconnected(user.id()));
        return summary;
    }

    /**
     * Lists all currently active lobbies.
     */
    @GetMapping("/lobbies")
    public ResponseEntity<Map<String, Object>> lobbies() {
        List<Map<String, Object>> lobbyList = lobbyService.getActiveLobbies().values().stream()
                .map(this::toLobbySummary)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", lobbyList.size());
        body.put("lobbies", lobbyList);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toLobbySummary(Lobby lobby) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", lobby.getId());
        summary.put("name", lobby.getName());
        summary.put("hostId", lobby.getHostId());
        summary.put("started", lobby.isStarted());
        summary.put("locked", lobby.isLocked());
        summary.put("playerCount", lobby.getUsers().size());
        List<Map<String, Object>> players = lobby.getUsers().stream()
                .map(u -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("id", u.id());
                    p.put("nickName", u.nickName());
                    p.put("ready", Boolean.TRUE.equals(lobby.getReadyStatus().get(u.id())));
                    p.put("role", lobby.getSelectedRoles().get(u.id()));
                    p.put("connected", !sessionAuthService.isUserDisconnected(u.id()));
                    return p;
                })
                .toList();
        summary.put("players", players);
        return summary;
    }

    /**
     * Lists all currently running games.
     */
    @GetMapping("/games")
    public ResponseEntity<Map<String, Object>> games() {
        List<Map<String, Object>> gameList = gameController.getActiveGames().values().stream()
                .map(this::toGameSummary)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", gameList.size());
        body.put("games", gameList);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> toGameSummary(GameState game) {
        Map<String, String> playerNames = game.getPlayerNames();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("gameId", game.getGameId());
        summary.put("hostId", game.getHostId());
        summary.put("currentRound", game.getCurrentRound());
        summary.put("currentPhase", game.getCurrentPhase());
        summary.put("playerCount", playerNames.size());
        summary.put("players", playerNames);
        summary.put("allPlayersHaveStartPosition", game.allPlayersHaveStartPosition());
        summary.put("disconnectedPlayers", playerNames.keySet().stream()
                .filter(sessionAuthService::isUserDisconnected)
                .toList());
        return summary;
    }
}