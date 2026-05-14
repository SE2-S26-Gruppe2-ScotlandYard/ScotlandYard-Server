package at.aau.serg.websocketdemoserver.dtos.lobby;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartGameMessage {
    private String lobbyId;
    private String requesterId; // must be host
}

