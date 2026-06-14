package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.KickPlayerInGameMessage;
import at.aau.serg.websocketdemoserver.dtos.game.RejoinGameMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.*;
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

    private UserConnectResponse registerUser(String nickname) {
        UserConnectMessage uc = new UserConnectMessage();
        uc.setNickName(nickname);
        return controller.handleUserConnect(uc);
    }

    private String createLobbyAndReturnId(String hostId, String hostNickname) {
        CreateLobbyMessage create = new CreateLobbyMessage();
        create.setLobbyName("TestLobby");
        create.setUserId(hostId);
        create.setNickName(hostNickname);
        controller.handleCreateLobby(create);
        // The controller broadcasts the lobby - we cannot easily get the ID here
        // Use a workaround by looking at the controller's internal state via service
        return null; // We'll test by rejoin with userId tracking
    }

    @Test
    void testHandleRejoinLobbyUnknownLobby() {
        RejoinLobbyMessage msg = new RejoinLobbyMessage();
        msg.setLobbyId("UNKNOWN");
        msg.setUserId("user1");
        msg.setNickName("Stefan");
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
    void testHandleRejoinGameWithNullGameId() {
        RejoinGameMessage msg = new RejoinGameMessage();
        msg.setGameId(null);
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
        UserConnectResponse host = registerUser("HostUser");
        CreateLobbyMessage create = new CreateLobbyMessage();
        create.setLobbyName("TestLobby");
        create.setUserId(host.getUser().id());
        create.setNickName("HostUser");
        controller.handleCreateLobby(create);

        RejoinLobbyMessage rejoin = new RejoinLobbyMessage();
        rejoin.setLobbyId("anything");
        rejoin.setUserId(host.getUser().id());
        rejoin.setNickName("HostUser");
        assertDoesNotThrow(() -> controller.handleRejoinLobby(rejoin));
    }

    @Test
    void testKickPlayerNoSession() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        msg.setGameId("");
        msg.setRequesterId("host1");
        msg.setTargetId("target1");
        assertDoesNotThrow(() -> controller.handleKickPlayerInGame(msg));
    }

    @Test
    void testMultipleRejoinAttemptsForSameUser() {
        UserConnectResponse user = registerUser("Stefan");
        RejoinLobbyMessage rejoin = new RejoinLobbyMessage();
        rejoin.setLobbyId("unknown");
        rejoin.setUserId(user.getUser().id());
        rejoin.setNickName("Stefan");

        assertDoesNotThrow(() -> {
            controller.handleRejoinLobby(rejoin);
            controller.handleRejoinLobby(rejoin);
            controller.handleRejoinLobby(rejoin);
        });
    }
}