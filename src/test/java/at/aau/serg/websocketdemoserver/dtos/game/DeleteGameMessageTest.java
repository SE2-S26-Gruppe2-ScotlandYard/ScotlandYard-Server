package at.aau.serg.websocketdemoserver.dtos.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeleteGameMessageTest {

    @Test
    void testGettersAndSetters() {
        DeleteGameMessage msg = new DeleteGameMessage();
        msg.setGameId("g1");
        msg.setRequesterId("host1");

        assertEquals("g1", msg.getGameId());
        assertEquals("host1", msg.getRequesterId());
    }

    @Test
    void testDefaultsNull() {
        DeleteGameMessage msg = new DeleteGameMessage();
        assertNull(msg.getGameId());
        assertNull(msg.getRequesterId());
    }
}
