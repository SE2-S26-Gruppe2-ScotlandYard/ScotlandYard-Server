package at.aau.serg.websocketdemoserver.dtos.lobby;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class KickPlayerMessage {
    private String lobbyId;
    private String requesterId;  // muss Host sein
    private String targetUserId; // wer gekickt wird
}