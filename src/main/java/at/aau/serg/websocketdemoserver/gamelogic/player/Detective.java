package at.aau.serg.websocketdemoserver.gamelogic.player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Detective extends Player{

    private final Map<TicketType, Integer> tickets;

    public Detective(String playerName, String playerId) {
        super(playerName, playerId);
        this.tickets = new EnumMap<>(TicketType.class);
        initializeTickets();
    }

    private void initializeTickets() {
        tickets.put(TicketType.WALKING, 10);
        tickets.put(TicketType.ESCOOTER, 8);
        tickets.put(TicketType.CARSHARING, 4);
    }

    @Override
    public boolean isMrX() {
        return false;
    }

    @Override
    public Map<TicketType, Integer> getTickets() {
        // Return a protected copy.
        return Collections.unmodifiableMap(tickets);
    }

    @Override
    public boolean hasTicket(TicketType ticketType) {
        return tickets.getOrDefault(ticketType, 0) > 0;
    }

    @Override
    public void useTicket(TicketType ticketType) {
        if (hasTicket(ticketType)) {
            tickets.put(ticketType, tickets.get(ticketType) - 1);
        }
    }
}
