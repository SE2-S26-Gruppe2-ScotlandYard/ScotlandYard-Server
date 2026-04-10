package at.aau.serg.websocketdemoserver.dtos.lobby;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLobbyMessage {
    private String lobbyName;
    private String userId;
    private String userName;
    private String password;
}