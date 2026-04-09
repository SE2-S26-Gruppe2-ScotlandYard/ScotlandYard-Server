package at.aau.serg.websocketdemoserver.dtos.lobby;

import at.aau.serg.websocketdemoserver.lobby.Lobby;

public class LobbyResponse {
    private boolean success;
    private String message;
    private String lobbyId;
    private Lobby lobby;

    public LobbyResponse(boolean success, String message, String lobbyId, Lobby lobby) {
        this.success = success;
        this.message = message;
        this.lobbyId = lobbyId;
        this.lobby = lobby;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public Lobby getLobby() {
        return lobby;
    }
}