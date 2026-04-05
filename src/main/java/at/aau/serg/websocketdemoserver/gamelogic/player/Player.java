package at.aau.serg.websocketdemoserver.gamelogic.player;

import java.util.Map;

public abstract class Player {

    protected String playerName;
    protected String playerId;
    private int currentPosition;

    public Player(String playerName, String playerId) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.currentPosition = 0; // 0 indicates no position assigned yet
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public abstract boolean isMrX();

    public abstract Map<TicketType, Integer> getTickets();

    public abstract boolean hasTicket(TicketType ticketType);

    public abstract void useTicket(TicketType ticketType);
}
