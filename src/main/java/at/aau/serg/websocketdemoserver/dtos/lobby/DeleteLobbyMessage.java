package at.aau.serg.websocketdemoserver.dtos.lobby;

public class DeleteLobbyMessage {
    private String lobbyId;
    private String requesterId;

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }
}