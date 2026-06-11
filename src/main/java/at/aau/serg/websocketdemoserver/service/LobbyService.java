package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    @FunctionalInterface
    interface LobbyFactory {
        Lobby create(String name, User host);
    }

    private final Map<String, Lobby> activeLobbies = new ConcurrentHashMap<>();
    private final LobbyFactory lobbyFactory;

    public LobbyService() {
        this(Lobby::new);
    }

    LobbyService(LobbyFactory factory) {
        this.lobbyFactory = factory;
    }

    public Lobby createLobby(String lobbyName, User host) {
        Lobby lobby = lobbyFactory.create(lobbyName, host);
        int attempts = 0;
        // Regenerate until the code is unique (collision is extremely rare)
        while (activeLobbies.containsKey(lobby.getId())) {
            if (++attempts > 100) {
                throw new IllegalStateException("Could not generate a unique lobby code");
            }
            lobby = lobbyFactory.create(lobbyName, host);
        }
        activeLobbies.put(lobby.getId(), lobby);
        return lobby;
    }

    public Lobby getLobby(String lobbyId) {
        return activeLobbies.get(lobbyId);
    }

    public Map<String, Lobby> getActiveLobbies() {
        return activeLobbies;
    }

    public Lobby joinLobby(String lobbyId, User user) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found");
        }

        boolean alreadyInLobby = lobby.getUsers().stream()
                .anyMatch(existingUser -> existingUser.id().equals(user.id()));

        if (alreadyInLobby) {
            throw new IllegalStateException("User already in lobby");
        }

        lobby.addUser(user);
        return lobby;
    }
    /**
     * Rejoin a lobby after disconnect.
     * If the player is already in the lobby, returns the lobby directly (no error).
     * If the player is not in the lobby anymore, adds them back.
     */
    public Lobby rejoinLobby(String lobbyId, User user) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found");
        }

        // Player is already in the lobby (e.g. just briefly disconnected)
        boolean alreadyInLobby = lobby.getUsers().stream()
                .anyMatch(existingUser -> existingUser.id().equals(user.id()));

        if (alreadyInLobby) {
            return lobby; // Just return the current lobby state
        }

        // Player was removed, add them back
        lobby.rejoinUser(user);
        return lobby;
    }

    public void leaveLobby(String lobbyId, String userId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found");
        }

        lobby.removeUser(userId);

        if (lobby.isEmpty()) {
            activeLobbies.remove(lobbyId);
        }
    }

    public void deleteLobby(String lobbyId, String requesterId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null) {
            throw new IllegalArgumentException("Lobby not found");
        }

        if (!lobby.getHostId().equals(requesterId)) {
            throw new IllegalStateException("Only host can delete lobby");
        }

        activeLobbies.remove(lobbyId);
    }

    public Lobby kickPlayer(String lobbyId, String requesterId, String targetUserId) {
        Lobby lobby = activeLobbies.get(lobbyId);
        if (lobby == null) throw new IllegalArgumentException("Lobby not found");
        if (!lobby.getHostId().equals(requesterId))
            throw new IllegalStateException("Only host can kick players");
        if (requesterId.equals(targetUserId))
            throw new IllegalStateException("Host cannot kick themselves");
        lobby.removeUser(targetUserId);
        return lobby;
    }

    public Lobby setRole(String lobbyId, String requesterId, String targetUserId, String role) {
        Lobby lobby = activeLobbies.get(lobbyId);
        if (lobby == null) throw new IllegalArgumentException("Lobby not found");
        // Spieler darf nur seine EIGENE Rolle setzen
        if (!requesterId.equals(targetUserId))
            throw new IllegalStateException("You can only set your own role");
        boolean success = lobby.selectRole(targetUserId,
                at.aau.serg.websocketdemoserver.lobby.Role.valueOf(role));
        if (!success) throw new IllegalStateException("Mr. X is already taken");
        lobby.markPlayerReady(targetUserId);
        return lobby;
    }


}

