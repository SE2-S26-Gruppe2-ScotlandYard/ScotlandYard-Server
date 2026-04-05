package at.aau.serg.websocketdemoserver.gamelogic.player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class MrX extends Player{

    private final Map<TicketType, Integer> tickets;

    public MrX(String playerName, String playerId) {
        super(playerName, playerId);
        this.tickets = new EnumMap<>(TicketType.class);
        initializeTickets();
    }

    private void initializeTickets() {
        // Mr. X has infinite regular tickets but a limited number of special tickets.
        tickets.put(TicketType.BLACK, 5);
        tickets.put(TicketType.DOUBLE, 2);
    }

    @Override
    public boolean isMrX() {
        return true;
    }

    @Override
    public Map<TicketType, Integer> getTickets() {
        // Return a protected copy.
        return Collections.unmodifiableMap(tickets);
    }

    @Override
    public boolean hasTicket(TicketType ticketType) {
        return switch (ticketType) {
            case WALKING, ESCOOTER, CARSHARING -> true;
            case BLACK, DOUBLE -> tickets.getOrDefault(ticketType, 0) > 0;
        };
    }

    @Override
    public void useTicket(TicketType ticketType) {
        if (hasTicket(ticketType)) {
            switch (ticketType) {
                case BLACK, DOUBLE -> tickets.put(ticketType, tickets.get(ticketType) - 1);
                // Regular tickets are infinite, so no action is needed here.
                case WALKING, ESCOOTER, CARSHARING -> {}
            }
        }
    }
}
