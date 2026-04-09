package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    private final Map<String, Lobby> activeLobbies = new ConcurrentHashMap<>();

    public Lobby createLobby(String lobbyName, User host) {
        Lobby lobby = new Lobby(lobbyName, host);
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


}

