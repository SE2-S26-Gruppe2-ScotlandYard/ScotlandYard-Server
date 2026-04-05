package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovementData {
    private String playerId;
    private String playerName;
    private TicketType ticketUsed;
    private int fromPosition;
    private int toPosition;
    private boolean isMrX;
}
