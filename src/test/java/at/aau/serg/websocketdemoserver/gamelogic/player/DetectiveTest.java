package at.aau.serg.websocketdemoserver.gamelogic.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectiveTest {

    private Detective detective;

    @BeforeEach
    void setUp() {
        detective = new Detective("Sherlock", "d1");
    }

    @Test
    void testDetectiveConstructor() {
        assertEquals("Sherlock", detective.getPlayerName());
        assertEquals("d1", detective.getPlayerId());
    }

    @Test
    void testIsMrX() {
        assertFalse(detective.isMrX());
    }

    @Test
    void testInitialTickets() {
        Map<TicketType, Integer> tickets = detective.getTickets();
        assertEquals(10, tickets.get(TicketType.WALKING));
        assertEquals(8, tickets.get(TicketType.ESCOOTER));
        assertEquals(4, tickets.get(TicketType.CARSHARING));
        assertNull(tickets.get(TicketType.BLACK));
    }

    @Test
    void testHasTicket() {
        assertTrue(detective.hasTicket(TicketType.WALKING));
    }

    @Test
    void testUseTicket() {
        int initialCount = detective.getTickets().get(TicketType.ESCOOTER);
        detective.useTicket(TicketType.ESCOOTER);
        assertEquals(initialCount - 1, detective.getTickets().get(TicketType.ESCOOTER));
    }

    @Test
    void testUseTicket_NoTicketsLeft() {
        // Detectives start with 10 walking tickets
        for (int i = 0; i < 10; i++) {
            assertTrue(detective.hasTicket(TicketType.WALKING));
            detective.useTicket(TicketType.WALKING);
        }

        // Now they should have 0 tickets left
        assertFalse(detective.hasTicket(TicketType.WALKING));
        assertEquals(0, detective.getTickets().get(TicketType.WALKING));

        // Using a ticket you don't have should not result in a negative count
        detective.useTicket(TicketType.WALKING);
        assertEquals(0, detective.getTickets().get(TicketType.WALKING));
    }

    @Test
    void testGetTickets_IsUnmodifiable() {
        Map<TicketType, Integer> tickets = detective.getTickets();
        assertThrows(UnsupportedOperationException.class, () -> tickets.put(TicketType.BLACK, 10));
    }
}
