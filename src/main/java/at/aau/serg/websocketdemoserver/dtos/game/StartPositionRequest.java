package at.aau.serg.websocketdemoserver.dtos.game;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a start position to a player.
 *
 * The optional field {@code selectedStartPosition} supports the cheat/debug
 * feature: when the spin-wheel is used manually the chosen position (1–199)
 * is submitted here.  When {@code null} the server falls back to its random
 * assignment logic (unchanged behaviour).
 */
@Data
@NoArgsConstructor
public class StartPositionRequest {

    private String gameId;
    private String playerId;

    /**
     * Optional manually-selected start position (cheat/debug feature).
     * Valid range: 1–199.  {@code null} triggers the automatic random fallback.
     */
    private Integer selectedStartPosition;

    /** Backward-compatible 2-arg constructor – no manual position. */
    public StartPositionRequest(String gameId, String playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.selectedStartPosition = null;
    }

    /** Full 3-arg constructor used when a manual start position is supplied. */
    public StartPositionRequest(String gameId, String playerId, Integer selectedStartPosition) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.selectedStartPosition = selectedStartPosition;
    }
}
