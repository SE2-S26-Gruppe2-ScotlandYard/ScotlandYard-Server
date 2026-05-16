package at.aau.serg.websocketdemoserver.dtos.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartPositionResponse {
    private String type;
    private String gameId;
    private String playerId;
    private Integer startPosition;
    private String message;
}
