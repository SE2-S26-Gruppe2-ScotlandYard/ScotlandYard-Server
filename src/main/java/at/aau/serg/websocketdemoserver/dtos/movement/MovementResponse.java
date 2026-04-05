package at.aau.serg.websocketdemoserver.dtos.movement;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class MovementResponse {
    private boolean success;
    private String message;
    private int newPosition;
    private MovementData movementData;
}

