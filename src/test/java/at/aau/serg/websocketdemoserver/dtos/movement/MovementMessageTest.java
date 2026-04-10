package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
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
        MovementMessage message = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(message.getGameId()).isEqualTo("game1");
        assertThat(message.getPlayerId()).isEqualTo("player1");
        assertThat(message.getTicket()).isEqualTo(TicketType.WALKING);
        assertThat(message.getTargetPosition()).isEqualTo(20);
        assertThat(message.getTimestamp()).isEqualTo(123L);
    }

    @Test
    void testSettersAndGetters() {
        MovementMessage message = new MovementMessage();

        message.setGameId("game2");
        message.setPlayerId("player2");
        message.setTicket(TicketType.WALKING);
        message.setTargetPosition(20);
        message.setTimestamp(123L);

        assertThat(message.getGameId()).isEqualTo("game2");
        assertThat(message.getPlayerId()).isEqualTo("player2");
        assertThat(message.getTicket()).isEqualTo(TicketType.WALKING);
        assertThat(message.getTargetPosition()).isEqualTo(20);
        assertThat(message.getTimestamp()).isEqualTo(123L);
    }
    @Test
    void testEqualsAndHashCode() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg3 = new MovementMessage("game2", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        assertThat(msg1).isNotEqualTo(msg3);
        assertThat(msg1).isNotEqualTo(null);
        assertThat(msg1).isNotEqualTo(new Object());
    }

    @Test
    void testCanEqual() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1.canEqual(msg2)).isTrue();
        assertThat(msg1.canEqual(new Object())).isFalse();
    }


    @Test
    void testEqualsSameObject() {
        MovementMessage message = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testEqualsDifferentClass() {
        MovementMessage message = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        assertThat(message.equals("string")).isFalse();
    }

    @Test
    void testEqualsDifferentGameId() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game2", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsDifferentPlayerId() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player2", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsDifferentTicket() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.ESCOOTER, 20, 123L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsDifferentTargetPos() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 10, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsDifferentTime() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 321L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void testEqualsWithNullGameId() {
        MovementMessage msg1 = new MovementMessage(null, "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage(null, "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg3 = new MovementMessage("game2", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1).isNotEqualTo(msg3);
    }

    @Test
    void testEqualsWithNullPlayerId() {
        MovementMessage msg1 = new MovementMessage("game1", null, TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", null, TicketType.WALKING, 20, 123L);
        MovementMessage msg3 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1).isNotEqualTo(msg3);
    }

    @Test
    void testEqualsWithNullTicket() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", null, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", null, 20, 123L);
        MovementMessage msg3 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1).isNotEqualTo(msg3);
    }

    @Test
    void testHashCodeConsistency() {
        MovementMessage msg1 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);
        MovementMessage msg2 = new MovementMessage("game1", "player1", TicketType.WALKING, 20, 123L);

        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
        assertThat(msg1.hashCode()).isEqualTo(msg1.hashCode()); // consistent
    }

    @Test
    void testHashCodeWithNullFields() {
        MovementMessage msg1 = new MovementMessage(null, null, null, 20, 123L);
        MovementMessage msg2 = new MovementMessage(null, null, null, 20, 123L);

        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }
}