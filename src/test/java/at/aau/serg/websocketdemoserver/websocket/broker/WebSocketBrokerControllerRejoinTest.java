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
    void testHandleRejoinGameWithBlankGameId() {
        RejoinGameMessage msg = new RejoinGameMessage();
        msg.setGameId("");
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
    void testHandleKickPlayerInGameWithNullGameId() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        msg.setGameId(null);
        msg.setRequesterId("host1");
        msg.setTargetId("target1");
        assertDoesNotThrow(() -> controller.handleKickPlayerInGame(msg));
    }

    @Test
    void testRejoinLobbyAfterRegisterAndCreate() {
        UserConnectMessage uc = new UserConnectMessage();
        uc.setNickName("hostxyz");
        UserConnectResponse host = controller.handleUserConnect(uc, null);
        assertNotNull(host.getUser(), "user registration should succeed");

        CreateLobbyMessage create = new CreateLobbyMessage();
        create.setLobbyName("TestLobby");
        create.setUserId(host.getUser().id());
        create.setNickName("hostxyz");
        controller.handleCreateLobby(create);

        RejoinLobbyMessage rejoin = new RejoinLobbyMessage();
        rejoin.setLobbyId("anything");
        rejoin.setUserId(host.getUser().id());
        rejoin.setNickName("hostxyz");
        assertDoesNotThrow(() -> controller.handleRejoinLobby(rejoin));
    }

    @Test
    void testRejoinGameAfterCreateLobby() {
        UserConnectMessage uc = new UserConnectMessage();
        uc.setNickName("hostzzz");
        UserConnectResponse host = controller.handleUserConnect(uc, null);
        assertNotNull(host.getUser());

        CreateLobbyMessage create = new CreateLobbyMessage();
        create.setLobbyName("TestLobby2");
        create.setUserId(host.getUser().id());
        create.setNickName("hostzzz");
        controller.handleCreateLobby(create);

        RejoinGameMessage msg = new RejoinGameMessage();
        msg.setGameId(null);
        msg.setUserId(host.getUser().id());
        assertDoesNotThrow(() -> controller.handleRejoinGame(msg));
    }

    @Test
    void testKickPlayerWithEmptyIds() {
        KickPlayerInGameMessage msg = new KickPlayerInGameMessage();
        msg.setGameId("");
        msg.setRequesterId("");
        msg.setTargetId("");
        assertDoesNotThrow(() -> controller.handleKickPlayerInGame(msg));
    }

    @Test
    void testRejoinGameForUnknownUser() {
        RejoinGameMessage msg = new RejoinGameMessage();
        msg.setGameId(null);
        msg.setUserId("completelyUnknownUserABC");
        assertDoesNotThrow(() -> controller.handleRejoinGame(msg));
    }

    @Test
    void testRejoinLobbyWithEmptyMessage() {
        RejoinLobbyMessage msg = new RejoinLobbyMessage();
        msg.setLobbyId("");
        msg.setUserId("");
        msg.setNickName("");
        assertDoesNotThrow(() -> controller.handleRejoinLobby(msg));
    }
    @Test
    void testHandleUserConnect_duplicateNickname_withDifferentSession_returnsError() {
        UserConnectMessage first = new UserConnectMessage("nick1", null);
        controller.handleUserConnect(first, "session-A");

        UserConnectMessage second = new UserConnectMessage("nick1", null);
        UserConnectResponse response = controller.handleUserConnect(second, "session-B");
        assertFalse(response.isSuccess());
        assertEquals("Nickname already taken", response.getMessage());
    }

    @Test
    void testHandleUserConnect_reconnect_withSameUserId_succeeds() {
        UserConnectMessage first = new UserConnectMessage("nick2", null);
        UserConnectResponse firstResponse = controller.handleUserConnect(first, "session-A");
        assertTrue(firstResponse.isSuccess());
        String userId = firstResponse.getUser().id();

        // Simulate reconnect: same nickname + same userId
        UserConnectMessage reconnect = new UserConnectMessage("nick2", userId);
        UserConnectResponse response = controller.handleUserConnect(reconnect, "session-B");
        assertTrue(response.isSuccess());
    }
}
