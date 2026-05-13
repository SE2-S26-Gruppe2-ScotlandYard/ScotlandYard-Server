package at.aau.serg.websocketdemoserver.dtos.lobby;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class SetRoleMessage {
    private String lobbyId;
    private String requesterId;  // muss Host sein
    private String targetUserId; // für wen die Rolle gesetzt wird
    private String role;         // "MRX" oder "DETECTIVE"
}