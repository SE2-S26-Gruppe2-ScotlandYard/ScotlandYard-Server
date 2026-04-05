package at.aau.serg.websocketdemoserver.gamelogic.player;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void testPlayerGettersAndSetters() {
        Player player = new Detective("Sherlock", "d1");

        assertEquals("Sherlock", player.getPlayerName());
        assertEquals("d1", player.getPlayerId());

        player.setPlayerName("Watson");
        player.setPlayerId("d2");

        assertEquals("Watson", player.getPlayerName());
        assertEquals("d2", player.getPlayerId());
    }
}
