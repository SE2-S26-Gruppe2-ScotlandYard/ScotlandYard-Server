package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.*;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WebSocketBrokerControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketBrokerController controller;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new WebSocketBrokerController(messagingTemplate);
    }

    // ── Bestehende Tests (angepasst) ──────────────────────────────────────

    @Test
    void testHandleUserConnect() {
        WebSocketBrokerController noMsgController = new WebSocketBrokerController(messagingTemplate);
        UserConnectMessage message = new UserConnectMessage();
        message.setNickName("Stefan");
        UserConnectResponse response = noMsgController.handleUserConnect(message);
        assertTrue(response.isSuccess());
        assertEquals("User registered", response.getMessage());
        assertNotNull(response.getUser().id());
        assertEquals("Stefan", response.getUser().nickName());
    }

    @Test
    void testHandleHello() {
        String response = controller.handleHello("test");
        assertEquals("echo from broker: test", response);
    }

    @Test
    void testHandleObject() {
        StompMessage message = new StompMessage("Stefan", "Hallo");
        StompMessage response = controller.handleObject(message);
        assertSame(message, response);
        assertEquals("Stefan", response.getFrom());
        assertEquals("Hallo", response.getText());
    }

    // ── Bewegungstests (angepasst auf void + verify) ─────────────────

    @Test
    void testHandleMove_NullMovement() {
        controller.handleMove("game1", null);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/game/game1/move-response"),
                argThat((MovementResponse r) -> !r.isSuccess() && "NULL MESSAGE".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleMove_NullPlayerId() {
        MovementMessage msg = new MovementMessage();
        msg.setGameId("game1");
        msg.setPlayerId(null);
        controller.handleMove("game1", msg);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/game/game1/move-response"),
                argThat((MovementResponse r) -> !r.isSuccess() && r.getMessage().contains("No player ID"))
        );
    }

    @Test
    void testHandleMove_GameNotFound() {
        MovementMessage msg = new MovementMessage();
        msg.setGameId("unknown");
        msg.setPlayerId("p1");
        controller.handleMove("unknown", msg);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/p1"),
                argThat((MovementResponse r) -> !r.isSuccess() && "Game not found".equals(r.getMessage()))
        );
    }

    // ── Lobby Tests (angepasste Topics) ─────────────────────────────

    @Test
    void testHandleCreateLobby_broadcastsToTopic() {
        CreateLobbyMessage message = new CreateLobbyMessage();
        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setNickName("Host");
        controller.handleCreateLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/lobby"),
                any(LobbyResponse.class)
        );
    }

    @Test
    void testHandleCreateLobby_broadcastsSuccessResponse() {
        CreateLobbyMessage message = new CreateLobbyMessage();
        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setNickName("Host");
        controller.handleCreateLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/lobby"),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "Lobby created".equals(r.getMessage())
                        && r.getLobby() != null
                        && "TestLobby".equals(r.getLobby().getName()))
        );
    }


    @Test
    void testHandleJoinLobby_broadcastsSuccess() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);

        var lobbyIdCaptor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), lobbyIdCaptor.capture());
        String lobbyId = lobbyIdCaptor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "Joined lobby".equals(r.getMessage())
                        && r.getLobby() != null
                        && r.getLobby().getUsers().size() == 2)
        );
    }

    @Test
    void testHandleJoinLobby_broadcastsErrorWhenLobbyNotFound() {
        JoinLobbyMessage message = new JoinLobbyMessage("missing-lobby", "2", "Player");
        controller.handleJoinLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/2"),
                argThat((LobbyResponse r) -> !r.isSuccess() && "Lobby not found".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleJoinLobby_broadcastsErrorOnInvalidUser() {
        JoinLobbyMessage message = new JoinLobbyMessage("someId", null, "Player");
        controller.handleJoinLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/null"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleLeaveLobby_broadcastsSuccess() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        LeaveLobbyMessage leaveMsg = new LeaveLobbyMessage(lobbyId, "2");
        controller.handleLeaveLobby(leaveMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess() && "Left lobby".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleLeaveLobby_deletesEmptyLobby() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        LeaveLobbyMessage leaveMsg = new LeaveLobbyMessage(lobbyId, "1");
        controller.handleLeaveLobby(leaveMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby"),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "Lobby deleted (empty)".equals(r.getMessage())
                        && r.getLobby() == null)
        );
    }

    @Test
    void testHandleLeaveLobby_broadcastsErrorWhenLobbyNotFound() {
        LeaveLobbyMessage message = new LeaveLobbyMessage("missing", "1");
        controller.handleLeaveLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleDeleteLobby_broadcastsSuccess() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        DeleteLobbyMessage deleteMsg = new DeleteLobbyMessage(lobbyId, "1");
        controller.handleDeleteLobby(deleteMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby"),
                argThat((LobbyResponse r) -> r.isSuccess() && "Lobby deleted".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleDeleteLobby_failsForNonHost() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        DeleteLobbyMessage deleteMsg = new DeleteLobbyMessage(lobbyId, "999");
        controller.handleDeleteLobby(deleteMsg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/999"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleDeleteLobby_broadcastsErrorWhenLobbyNotFound() {
        DeleteLobbyMessage message = new DeleteLobbyMessage("missing", "1");
        controller.handleDeleteLobby(message);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    // ── KickPlayer ─────────────────────────────────────────────

    @Test
    void testHandleKickPlayer_success() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        KickPlayerMessage kickMsg = new KickPlayerMessage(lobbyId, "1", "2");
        controller.handleKickPlayer(kickMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "Player kicked".equals(r.getMessage())
                        && r.getLobby() != null
                        && r.getLobby().getUsers().size() == 1)
        );
    }

    @Test
    void testHandleKickPlayer_failsForNonHost() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        KickPlayerMessage kickMsg = new KickPlayerMessage(lobbyId, "2", "1");
        controller.handleKickPlayer(kickMsg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/2"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleKickPlayer_broadcastsErrorWhenLobbyNotFound() {
        KickPlayerMessage msg = new KickPlayerMessage("missing", "1", "2");
        controller.handleKickPlayer(msg);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    // ── SetRole ────────────────────────────────────────────────

    @Test
    void testHandleSetRole_playerSetsOwnRole() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        SetRoleMessage roleMsg = new SetRoleMessage(lobbyId, "1", "1", "MRX");
        controller.handleSetRole(roleMsg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess() && "Role set".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleSetRole_failsWhenSettingOtherPlayerRole() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        SetRoleMessage roleMsg = new SetRoleMessage(lobbyId, "1", "2", "DETECTIVE");
        controller.handleSetRole(roleMsg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleSetRole_broadcastsErrorWhenLobbyNotFound() {
        SetRoleMessage msg = new SetRoleMessage("missing", "1", "1", "MRX");
        controller.handleSetRole(msg);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    // ── StartRoleSelection ────────────────────────────────────

    @Test
    void testHandleStartRoleSelection_hostCanStart() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        StartRoleSelectionMessage msg = new StartRoleSelectionMessage(lobbyId, "1");
        controller.handleStartRoleSelection(msg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "ROLE_SELECTION_STARTED".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleStartRoleSelection_failsForNonHost() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        JoinLobbyMessage joinMsg = new JoinLobbyMessage(lobbyId, "2", "Player");
        controller.handleJoinLobby(joinMsg);

        StartRoleSelectionMessage msg = new StartRoleSelectionMessage(lobbyId, "2");
        controller.handleStartRoleSelection(msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/2"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleStartRoleSelection_broadcastsErrorWhenLobbyNotFound() {
        StartRoleSelectionMessage msg = new StartRoleSelectionMessage("missing", "1");
        controller.handleStartRoleSelection(msg);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }
}
