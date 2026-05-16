package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameController {

    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();

    public void addGame(String gameId, GameState gameState) {
        activeGames.put(gameId, gameState);
        printAllGames();
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void removeGame(String gameId) {
        activeGames.remove(gameId);
    }

    //TEST
    private void printAllGames() {
        System.out.println("\n=== AKTUELL LAUFENDE SPIELE (" + activeGames.size() + ") ===");
        if (activeGames.isEmpty()) {
            System.out.println("Keine Spiele laufend.");
        } else {
            for (GameState g : activeGames.values()) {
                System.out.println("- ID: " + g.getGameId());
            }
        }
        System.out.println("=====================================\n");
    }
//TEST
}