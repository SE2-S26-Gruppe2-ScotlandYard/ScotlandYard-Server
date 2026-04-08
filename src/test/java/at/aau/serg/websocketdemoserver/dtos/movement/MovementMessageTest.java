package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MovementMessageTest {

    @Test
    void testNoArgsConstructor() {
        MovementMessage message = new MovementMessage();

        assertThat(message.getGameId()).isNull();
        assertThat(message.getPlayerId()).isNull();
        assertThat(message.getTicket()).isNull();
        assertThat(message.getTargetPosition()).isZero();
        assertThat(message.getTimestamp()).isZero();
    }

    @Test
    void testAllArgsConstructor() {
        long timestamp = System.currentTimeMillis();
        MovementMessage message = new MovementMessage("game1", "player1", TicketType.WALKING, 20, timestamp);

        assertThat(message.getGameId()).isEqualTo("game1");
        assertThat(message.getPlayerId()).isEqualTo("player1");
        assertThat(message.getTicket()).isEqualTo(TicketType.WALKING);
        assertThat(message.getTargetPosition()).isEqualTo(20);
        assertThat(message.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void testSettersAndGetters() {
        MovementMessage message = new MovementMessage();

        long timestamp = 123456789L;
        message.setGameId("game2");
        message.setPlayerId("player2");
        message.setTicket(TicketType.WALKING);
        message.setTargetPosition(20);
        message.setTimestamp(timestamp);

        assertThat(message.getGameId()).isEqualTo("game2");
        assertThat(message.getPlayerId()).isEqualTo("player2");
        assertThat(message.getTicket()).isEqualTo(TicketType.WALKING);
        assertThat(message.getTargetPosition()).isEqualTo(20);
        assertThat(message.getTimestamp()).isEqualTo(timestamp);
    }
}