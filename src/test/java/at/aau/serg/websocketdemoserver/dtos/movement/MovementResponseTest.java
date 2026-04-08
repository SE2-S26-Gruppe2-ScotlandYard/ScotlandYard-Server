package at.aau.serg.websocketdemoserver.dtos.movement;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;

class MovementResponseTest {

    @Test
    void testNoArgsConstructor() {
        MovementResponse response = new MovementResponse();

        assertThat(response.isSuccess()).isFalse(); // boolean default false
        assertThat(response.getMessage()).isNull();
        assertThat(response.getNewPosition()).isZero();
        assertThat(response.getMovementData()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse response = new MovementResponse(true, "Movement successful", 20, movementData);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Movement successful");
        assertThat(response.getNewPosition()).isEqualTo(20);
        assertThat(response.getMovementData()).isEqualTo(movementData);
    }

    @Test
    void testSettersAndGetters() {
        MovementResponse response = new MovementResponse();

        MovementData movementData = new MovementData("player2", TicketType.WALKING, 2, 20, true);
        response.setSuccess(true);
        response.setMessage("message");
        response.setNewPosition(20);
        response.setMovementData(movementData);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("message");
        assertThat(response.getNewPosition()).isEqualTo(20);
        assertThat(response.getMovementData()).isEqualTo(movementData);
    }
}