package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovementMessage {
    private String gameId;
    private String playerId;
    private String playerName;
    private TicketType ticket;
    private int targetPosition;
    private long timestamp;
}