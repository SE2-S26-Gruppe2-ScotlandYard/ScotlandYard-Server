package at.aau.serg.websocketdemoserver.dtos.lobby;

import at.aau.serg.websocketdemoserver.lobby.User;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConnectResponse {
    private boolean success;
    private String message;
    private User user;
}