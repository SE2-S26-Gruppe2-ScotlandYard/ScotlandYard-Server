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
}