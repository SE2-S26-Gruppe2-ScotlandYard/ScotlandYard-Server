package at.aau.serg.websocketdemoserver.dtos.movement;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovementResponse {
    private boolean success;
    private String message;
    private int newPosition;
    private MovementData movementData;
}

