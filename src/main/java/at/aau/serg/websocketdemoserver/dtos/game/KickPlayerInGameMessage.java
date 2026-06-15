package at.aau.serg.websocketdemoserver.dtos.game;

public class KickPlayerInGameMessage {
    private String gameId;
    private String requesterId;
    private String targetId;

    public KickPlayerInGameMessage() {}

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}