package at.aau.serg.websocketdemoserver.gamelogic.player;

import java.util.Map;

public abstract class Player {

    protected String playerName;
    protected String playerId;

    public Player(String playerName, String playerId) {
        this.playerName = playerName;
        this.playerId = playerId;
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

    public abstract boolean isMrX();

    public abstract Map<TicketType, Integer> getTickets();

    public abstract boolean hasTicket(TicketType ticketType);

    public abstract void useTicket(TicketType ticketType);
}
