package at.aau.serg.websocketdemoserver.dtos.movement;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;

class MovementResponseTest {

    @Test
    void testNoArgsConstructor() {
        MovementResponse responseEmpty = new MovementResponse();
        assertThat(responseEmpty.isSuccess()).isFalse(); // boolean default false
        assertThat(responseEmpty.getMessage()).isNull();
        assertThat(responseEmpty.getNewPosition()).isZero();
        assertThat(responseEmpty.getMovementData()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        assertThat(responseSuccess.isSuccess()).isTrue();
        assertThat(responseSuccess.getMessage()).isEqualTo("Movement successful");
        assertThat(responseSuccess.getNewPosition()).isEqualTo(20);
        assertThat(responseSuccess.getMovementData()).isEqualTo(movementData);
    }

    @Test
    void testSettersAndGetters() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseEmpty = new MovementResponse();
        responseEmpty.setSuccess(true);
        responseEmpty.setMessage("message");
        responseEmpty.setNewPosition(20);
        responseEmpty.setMovementData(movementData);

        assertThat(responseEmpty.isSuccess()).isTrue();
        assertThat(responseEmpty.getMessage()).isEqualTo("message");
        assertThat(responseEmpty.getNewPosition()).isEqualTo(20);
        assertThat(responseEmpty.getMovementData()).isEqualTo(movementData);
    }

    @Test
    void testEqualsSameObject() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        assertThat(responseSuccess.equals(responseSuccess)).isTrue();
    }

    @Test
    void testEqualsDifferentClass() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        assertThat(responseSuccess.equals("string")).isFalse();
    }

    @Test
    void testEqualsDifferentFrom() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        MovementResponse responseEmpty = new MovementResponse();
        responseEmpty.setSuccess(true);
        responseEmpty.setMessage("Movement successful");
        responseEmpty.setNewPosition(21);
        responseEmpty.setMovementData(movementData);
        assertThat(responseSuccess).isNotEqualTo(responseEmpty);    //
    }

    @Test
    void testEqualsDifferentText() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        MovementResponse responseEmpty = new MovementResponse();
        responseEmpty.setSuccess(true);
        responseEmpty.setMessage("message");
        responseEmpty.setNewPosition(20);
        responseEmpty.setMovementData(movementData);
        assertThat(responseSuccess).isNotEqualTo(responseEmpty);    //
    }

    @Test
    void testEqualsWithNullMessage() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        MovementResponse response1 = new MovementResponse(true, null, 20, movementData);
        MovementResponse response2 = new MovementResponse(true, null, 20, movementData);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(responseSuccess);
    }

    @Test
    void testEqualsWithNullMovementData() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        MovementResponse response1 = new MovementResponse(true, "Hello", 20, null);
        MovementResponse response2 = new MovementResponse(true, "Hello", 20, null);

        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(responseSuccess);
    }

    @Test
    void testHashCodeConsistency() {
        MovementData movementData = new MovementData("player1", TicketType.WALKING, 2, 20, false);
        MovementResponse responseSuccess = new MovementResponse(true, "Movement successful", 20, movementData);
        MovementResponse response1 = new MovementResponse(true, "Hello", 20, movementData);
        MovementResponse response2 = new MovementResponse(true, "Hello", 20, movementData);

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.hashCode()).isEqualTo(response1.hashCode()); // consistent
    }

    @Test
    void testHashCodeWithNullFields() {
        MovementResponse response1 = new MovementResponse(true, null, 20, null);
        MovementResponse response2 = new MovementResponse(true, null, 20, null);

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
}