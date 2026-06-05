package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Simple HTTP health endpoint. */
@RestController
@RequestMapping("/health")
public class HealthController {

    private final GameController gameController;
    private final LobbyService lobbyService;

    @Autowired
    public HealthController(GameController gameController, LobbyService lobbyService) {
        this.gameController = gameController;
        this.lobbyService = lobbyService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "activeGames", gameController.getActiveGamesCount(),
                "activeLobbies", lobbyService.getActiveLobbies().size()
        );
        return ResponseEntity.ok(body);
    }
}