package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MovementDataTest {

    @Test
    void testNoArgsConstructor() {
        MovementData data = new MovementData();

        assertThat(data.getPlayerId()).isNull();
        assertThat(data.getTicketUsed()).isNull();
        assertThat(data.getFromPosition()).isZero();
        assertThat(data.getToPosition()).isZero();
        assertThat(data.isMrX()).isFalse();
    }

    @Test
    void testAllArgsConstructor() {
        MovementData data = new MovementData(
                "player1",
                TicketType.WALKING,
                2,
                20,
                true
        );

        assertThat(data.getPlayerId()).isEqualTo("player1");
        assertThat(data.getTicketUsed()).isEqualTo(TicketType.WALKING);
        assertThat(data.getFromPosition()).isEqualTo(2);
        assertThat(data.getToPosition()).isEqualTo(20);
        assertThat(data.isMrX()).isTrue();
    }

    @Test
    void testSettersAndGetters() {
        MovementData data = new MovementData();

        data.setPlayerId("player2");
        data.setTicketUsed(TicketType.WALKING);
        data.setFromPosition(2);
        data.setToPosition(20);
        data.setMrX(false);

        assertThat(data.getPlayerId()).isEqualTo("player2");
        assertThat(data.getTicketUsed()).isEqualTo(TicketType.WALKING);
        assertThat(data.getFromPosition()).isEqualTo(2);
        assertThat(data.getToPosition()).isEqualTo(20);
        assertThat(data.isMrX()).isFalse();
    }

    @Test
    void testEqualsAndHashCode() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data3 = new MovementData("player2", TicketType.WALKING, 2, 20, true);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
        assertThat(data1).isNotEqualTo(data3);
        assertThat(data1).isNotEqualTo(null);
        assertThat(data1).isNotEqualTo(new Object());
    }

    @Test
    void testCanEqual() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);

        assertThat(data1.canEqual(data2)).isTrue();
        assertThat(data1.canEqual(new Object())).isFalse();
    }


    @Test
    void testEqualsSameObject() {
        MovementData data = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        assertThat(data.equals(data)).isTrue();
    }

    @Test
    void testEqualsDifferentClass() {
        MovementData data = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        assertThat(data.equals("string")).isFalse();
    }

    @Test
    void testEqualsDifferentPlayerId() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData("player2", TicketType.WALKING, 2, 20, true);
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEqualsDifferentTicket() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData("player1", TicketType.ESCOOTER, 2, 20, true);
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEqualsDifferentFrom() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 1, 20, true);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEqualsDifferentTo() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 10, true);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEqualsDifferentMrx() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        assertThat(data1).isNotEqualTo(data2);
    }

    @Test
    void testEqualsWithNullPlayerID() {
        MovementData data1 = new MovementData(null, TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData(null, TicketType.WALKING, 2, 20, true);
        MovementData data3 = new MovementData("player1", TicketType.WALKING, 2, 20, true);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1).isNotEqualTo(data3);
    }

    @Test
    void testEqualsWithNullTicket() {
        MovementData data1 = new MovementData("player1", null, 2, 20, true);
        MovementData data2 = new MovementData("player1", null, 2, 20, true);
        MovementData data3 = new MovementData("player1", TicketType.WALKING, 2, 20, true);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1).isNotEqualTo(data3);
    }

    @Test
    void testHashCodeConsistency() {
        MovementData data1 = new MovementData("player1", TicketType.WALKING, 2, 20, true);
        MovementData data2 = new MovementData("player1", TicketType.WALKING, 2, 20, true);

        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
        assertThat(data1.hashCode()).isEqualTo(data1.hashCode()); // consistent
    }

    @Test
    void testHashCodeWithNullFields() {
        MovementData data1 = new MovementData(null, null, 2, 20, true);
        MovementData data2 = new MovementData(null, null, 2, 20, true);

        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }
}