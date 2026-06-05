package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameController {

    private static GameController gameController;
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();

    public void addGame(String gameId, GameState gameState) {
        activeGames.put(gameId, gameState);
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(String gameId) {
        activeGames.remove(gameId);
    }

    public static GameController getInstance() {
        if (gameController == null) {
            gameController = new GameController();
        }
        return gameController;
    }

    public int getActiveGamesCount() {
        return activeGames.size();
    }
}