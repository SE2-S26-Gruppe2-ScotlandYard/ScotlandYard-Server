package at.aau.serg.websocketdemoserver.dtos.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STOMP message sent by the client to confirm its chosen start position.
 *
 * <p>Destination: {@code /app/game/start-position/confirm}
 *
 * <p>The client (frontend spinner) generates the position locally and sends
 * it here for server-side validation and persistence.  The server validates
 * that the position is in range (1–199) and not already occupied, then
 * broadcasts the updated {@code GameStateDto} to
 * {@code /topic/game/{gameId}/movements} so every player on the board
 * screen sees the new position immediately.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartPositionConfirmRequest {
    private String gameId;
    private String playerId;
    /** Chosen start position, must be in range 1–199. */
    private int startPosition;
}

