package at.aau.serg.websocketdemoserver.dtos.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejoinGameMessage {
    private String gameId;
    private String userId;
}