package at.aau.serg.websocketdemoserver.gamelogic.player;

import java.util.Map;

import at.aau.serg.websocketdemoserver.lobby.User;
import lombok.Getter;
import lombok.Setter;

public abstract class Player {

    @Setter
    @Getter
    protected String playerName;
    @Setter
    @Getter
    protected String playerId;
    protected final User user;

    protected Player(User user) {
        this.playerName = user.name();
        this.playerId = user.id();
        this.user = user;
    }

    public abstract boolean isMrX();

    public abstract Map<TicketType, Integer> getTickets();

    public abstract boolean hasTicket(TicketType ticketType);

    public abstract void useTicket(TicketType ticketType);
}
