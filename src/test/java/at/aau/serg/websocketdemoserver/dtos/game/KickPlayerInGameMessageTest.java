package at.aau.serg.websocketdemoserver.dtos.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KickPlayerInGameMessageTest {

    @Test
    void testGettersAndSetters() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        msg.setGameId("g1");
        msg.setRequesterId("host1");
        msg.setTargetId("target1");

        assertEquals("g1", msg.getGameId());
        assertEquals("host1", msg.getRequesterId());
        assertEquals("target1", msg.getTargetId());
    }

    @Test
    void testDefaultsNull() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        assertNull(msg.getGameId());
        assertNull(msg.getRequesterId());
        assertNull(msg.getTargetId());
    }
}