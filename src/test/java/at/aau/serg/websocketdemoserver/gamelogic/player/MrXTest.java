package at.aau.serg.websocketdemoserver.gamelogic.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import at.aau.serg.websocketdemoserver.lobby.User;

class MrXTest {

    private MrX mrX;

    @BeforeEach
    void setUp() {
        mrX = new MrX(new User("x1", "Moriarty", "23"));
    }

    @Test
    void testMrXConstructor() {
        assertEquals("Moriarty", mrX.getPlayerName());
        assertEquals("x1", mrX.getPlayerId());
    }

    @Test
    void testIsMrX() {
        assertTrue(mrX.isMrX());
    }

    @Test
    void testInitialTickets() {
        Map<TicketType, Integer> tickets = mrX.getTickets();
        assertEquals(5, tickets.get(TicketType.BLACK));
        assertEquals(2, tickets.get(TicketType.DOUBLE));
        assertNull(tickets.get(TicketType.WALKING)); // Regular tickets are infinite
    }

    @Test
    void testHasTicket_InfiniteTickets() {
        assertTrue(mrX.hasTicket(TicketType.WALKING));
        assertTrue(mrX.hasTicket(TicketType.ESCOOTER));
        assertTrue(mrX.hasTicket(TicketType.CARSHARING));
    }

    @Test
    void testUseTicket_InfiniteTickets() {
        // Using infinite tickets should not change the map
        int blackTicketsBefore = mrX.getTickets().get(TicketType.BLACK);
        int doubleTicketsBefore = mrX.getTickets().get(TicketType.DOUBLE);

        mrX.useTicket(TicketType.WALKING);

        assertEquals(blackTicketsBefore, mrX.getTickets().get(TicketType.BLACK));
        assertEquals(doubleTicketsBefore, mrX.getTickets().get(TicketType.DOUBLE));
    }

    @Test
    void testUseTicket_LimitedTickets() {
        int initialBlack = mrX.getTickets().get(TicketType.BLACK);
        mrX.useTicket(TicketType.BLACK);
        assertEquals(initialBlack - 1, mrX.getTickets().get(TicketType.BLACK));
    }

    @Test
    void testUseTicket_NoLimitedTicketsLeft() {
        // Mr. X starts with 5 BLACK tickets
        for (int i = 0; i < 5; i++) {
            assertTrue(mrX.hasTicket(TicketType.BLACK));
            mrX.useTicket(TicketType.BLACK);
        }

        // Now they should have 0 tickets left
        assertFalse(mrX.hasTicket(TicketType.BLACK));
        assertEquals(0, mrX.getTickets().get(TicketType.BLACK));

        // Using a ticket you don't have should not result in a negative count
        mrX.useTicket(TicketType.BLACK);
        assertEquals(0, mrX.getTickets().get(TicketType.BLACK));
    }

    @Test
    void testGetTickets_IsUnmodifiable() {
        Map<TicketType, Integer> tickets = mrX.getTickets();
        assertThrows(UnsupportedOperationException.class, () -> tickets.put(TicketType.WALKING, 10));
    }
}
