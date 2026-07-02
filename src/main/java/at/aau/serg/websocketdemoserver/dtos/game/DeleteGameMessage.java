package at.aau.serg.websocketdemoserver.dtos.game;

public class DeleteGameMessage {
    private String gameId;
    private String requesterId;

    public DeleteGameMessage() {}

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
}