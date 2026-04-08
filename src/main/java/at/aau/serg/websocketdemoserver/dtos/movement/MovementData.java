package at.aau.serg.websocketdemoserver.dtos.movement;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data   //NOSONAR
@AllArgsConstructor
@NoArgsConstructor
public class MovementData {
    private String playerId;
    private TicketType ticketUsed;
    private int fromPosition;
    private int toPosition;
    private boolean isMrX;
}
