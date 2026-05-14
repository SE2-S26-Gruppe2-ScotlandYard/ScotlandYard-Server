package at.aau.serg.websocketdemoserver.dtos.lobby;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BackToLobbyMessage {
    @JsonProperty("lobbyId")
    private String lobbyId;

    @JsonProperty("requesterId")
    private String requesterId;
}
