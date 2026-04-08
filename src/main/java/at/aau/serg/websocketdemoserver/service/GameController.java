package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameController {

    private static GameController controllerInstance = null;
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();

    public static GameController getInstance() {
        if (controllerInstance == null) {
            controllerInstance = new GameController();
        }
        return controllerInstance;
    }
    public void addGame(String gameId, GameState gameState) {
        activeGames.put(gameId, gameState);
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(String gameId) {
        activeGames.remove(gameId);
    }
}