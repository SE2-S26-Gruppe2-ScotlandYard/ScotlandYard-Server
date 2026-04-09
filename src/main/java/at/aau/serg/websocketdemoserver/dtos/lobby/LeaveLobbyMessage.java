package at.aau.serg.websocketdemoserver.dtos.lobby;

public class LeaveLobbyMessage {
    private String lobbyId;
    private String userId;

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}