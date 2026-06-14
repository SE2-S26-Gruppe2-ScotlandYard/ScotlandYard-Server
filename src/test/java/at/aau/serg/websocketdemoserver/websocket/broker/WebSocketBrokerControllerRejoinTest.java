package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.KickPlayerInGameMessage;
import at.aau.serg.websocketdemoserver.dtos.game.RejoinGameMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.RejoinLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.CreateLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.UserConnectMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.UserConnectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketBrokerControllerRejoinTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketBrokerController controller;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new WebSocketBrokerController(messagingTemplate);
    }

    @Test
    void testHandleRejoinLobbyUnknownLobby() {
        RejoinLobbyMessage msg = new RejoinLobbyMessage();
        msg.setLobbyId("UNKNOWN");
        msg.setUserId("user1");
        msg.setNickName("Stefan");

        // Should not throw, just send error
        assertDoesNotThrow(() -> controller.handleRejoinLobby(msg));
    }

    @Test
    void testHandleRejoinGameUnknownGame() {
        RejoinGameMessage msg = new RejoinGameMessage();
        msg.setGameId("UNKNOWN");
        msg.setUserId("user1");

        assertDoesNotThrow(() -> controller.handleRejoinGame(msg));
    }

    @Test
    void testHandleKickPlayerInGameUnknownGame() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        msg.setGameId("UNKNOWN");
        msg.setRequesterId("host1");
        msg.setTargetId("target1");

        assertDoesNotThrow(() -> controller.handleKickPlayerInGame(msg));
    }

    @Test
    void testRejoinLobbyAfterRegisterAndCreate() {
        UserConnectMessage uc = new UserConnectMessage();
        uc.setNickName("Stefan");
        UserConnectResponse ucResp = controller.handleUserConnect(uc);

        CreateLobbyMessage create = new CreateLobbyMessage();
        create.setLobbyName("TestLobby");
        create.setUserId(ucResp.getUser().id());
        controller.handleCreateLobby(create);

        // Try rejoin
        RejoinLobbyMessage rejoin = new RejoinLobbyMessage();
        rejoin.setLobbyId("anything");
        rejoin.setUserId(ucResp.getUser().id());
        rejoin.setNickName("Stefan");
        assertDoesNotThrow(() -> controller.handleRejoinLobby(rejoin));
    }
}