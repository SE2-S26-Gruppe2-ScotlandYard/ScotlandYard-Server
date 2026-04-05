package at.aau.serg.websocketdemoserver.dtos.movement;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovementMessage {
    private String gameId;
    private MovementData movementData;
    private long timestamp;
}