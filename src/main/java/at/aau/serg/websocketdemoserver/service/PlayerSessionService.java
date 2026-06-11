package at.aau.serg.websocketdemoserver.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which lobby and game each player is currently in.
 * Used for Game State Recovery on disconnect/reconnect.
 */
@Service
public class PlayerSessionService {

    // userId -> lobbyId
    private final Map<String, String> playerLobbyMap = new ConcurrentHashMap<>();

    // userId -> gameId
    private final Map<String, String> playerGameMap = new ConcurrentHashMap<>();

    // ── Lobby tracking ─────────────────────────────────────────────────────

    public void playerJoinedLobby(String userId, String lobbyId) {
        playerLobbyMap.put(userId, lobbyId);
    }

    public void playerLeftLobby(String userId) {
        playerLobbyMap.remove(userId);
    }

    public String getLobbyForPlayer(String userId) {
        return playerLobbyMap.get(userId);
    }

    public boolean isPlayerInLobby(String userId) {
        return playerLobbyMap.containsKey(userId);
    }

    // ── Game tracking ──────────────────────────────────────────────────────

    public void playerJoinedGame(String userId, String gameId) {
        playerGameMap.put(userId, gameId);
        // Player is in a game, no need to track lobby anymore
        playerLobbyMap.remove(userId);
    }

    public void playerLeftGame(String userId) {
        playerGameMap.remove(userId);
    }

    public String getGameForPlayer(String userId) {
        return playerGameMap.get(userId);
    }

    public boolean isPlayerInGame(String userId) {
        return playerGameMap.containsKey(userId);
    }

    // ── Full cleanup ───────────────────────────────────────────────────────

    public void playerDisconnected(String userId) {
        // Do NOT remove from maps on disconnect!
        // We keep the mapping so the player can rejoin.
    }

    public void playerFullyLeft(String userId) {
        playerLobbyMap.remove(userId);
        playerGameMap.remove(userId);
    }
}