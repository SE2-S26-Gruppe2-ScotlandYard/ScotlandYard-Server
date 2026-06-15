package at.aau.serg.websocketdemoserver.dtos.lobby;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejoinLobbyMessage {
    private String lobbyId;
    private String userId;
    private String nickName;
}