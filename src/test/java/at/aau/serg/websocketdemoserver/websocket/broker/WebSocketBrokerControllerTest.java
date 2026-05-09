package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StartPositionRequest;
import at.aau.serg.websocketdemoserver.dtos.StartPositionResponse;
import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.CreateLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.DeleteLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.JoinLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LeaveLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LobbyResponse;
import at.aau.serg.websocketdemoserver.dtos.lobby.UserConnectMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.UserConnectResponse;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebSocketBrokerControllerTest {

    @Test
    void testHandleUserConnect() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        UserConnectMessage message = new UserConnectMessage();
        message.setNickName("Stefan");

        UserConnectResponse response = controller.handleUserConnect(message);

        assertTrue(response.isSuccess());
        assertEquals("User registered", response.getMessage());
        assertNotNull(response.getUser().id());
        assertEquals("Stefan", response.getUser().nickName());
    }

    @Test
    void testHandleHello() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        String response = controller.handleHello("test");

        assertEquals("echo from broker: test", response);
    }

    @Test
    void testHandleObject() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        StompMessage message = new StompMessage("Stefan", "Hallo");

        StompMessage response = controller.handleObject(message);

        assertSame(message, response);
        assertEquals("Stefan", response.getFrom());
        assertEquals("Hallo", response.getText());
    }

    @Test
    void testHandleMoveReturnsInvalidWhenGameIdIsNull() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        MovementMessage message = new MovementMessage();
        message.setGameId(null);
        message.setPlayerId("player-1");
        message.setTicket(TicketType.WALKING);
        message.setTargetPosition(42);

        MovementResponse response = controller.handleMove(message);

        assertFalse(response.isSuccess());
        assertEquals("Invalid movement data", response.getMessage());
        assertEquals(0, response.getNewPosition());
        assertNull(response.getMovementData());
    }

    @Test
    void testHandleMoveReturnsInvalidWhenPlayerIdIsNull() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        MovementMessage message = new MovementMessage();
        message.setGameId("game-1");
        message.setPlayerId(null);
        message.setTicket(TicketType.WALKING);
        message.setTargetPosition(42);

        MovementResponse response = controller.handleMove(message);

        assertFalse(response.isSuccess());
        assertEquals("Invalid movement data", response.getMessage());
        assertEquals(0, response.getNewPosition());
        assertNull(response.getMovementData());
    }

    @Test
    void testHandleMoveReturnsGameNotFound() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        MovementMessage message = new MovementMessage();
        message.setGameId("unknown-game");
        message.setPlayerId("player-1");
        message.setTicket(TicketType.WALKING);
        message.setTargetPosition(42);

        MovementResponse response = controller.handleMove(message);

        assertFalse(response.isSuccess());
        assertEquals("Game not found", response.getMessage());
        assertEquals(0, response.getNewPosition());
        assertNull(response.getMovementData());
    }

    @Test
    void testHandleCreateLobby() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        CreateLobbyMessage message = new CreateLobbyMessage();
        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setNickName("Host");

        LobbyResponse response = controller.handleCreateLobby(message);

        assertTrue(response.isSuccess());
        assertEquals("Lobby created", response.getMessage());
        assertNotNull(response.getLobbyId());
        assertNotNull(response.getLobby());
        assertEquals("TestLobby", response.getLobby().getName());
        assertEquals("1", response.getLobby().getHostId());
    }

    @Test
    void testHandleJoinLobby() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        CreateLobbyMessage createMessage = new CreateLobbyMessage();
        createMessage.setLobbyName("TestLobby");
        createMessage.setUserId("1");
        createMessage.setNickName("Host");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        JoinLobbyMessage joinMessage = new JoinLobbyMessage();
        joinMessage.setLobbyId(createResponse.getLobbyId());
        joinMessage.setUserId("2");
        joinMessage.setNickName("Player");

        LobbyResponse joinResponse = controller.handleJoinLobby(joinMessage);

        assertTrue(joinResponse.isSuccess());
        assertEquals("Joined lobby", joinResponse.getMessage());
        assertNotNull(joinResponse.getLobby());
        assertEquals(2, joinResponse.getLobby().getUsers().size());
    }

    @Test
    void testHandleJoinLobbyFailsWhenLobbyDoesNotExist() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        JoinLobbyMessage joinMessage = new JoinLobbyMessage();
        joinMessage.setLobbyId("missing-lobby");
        joinMessage.setUserId("2");
        joinMessage.setNickName("Player");

        LobbyResponse response = controller.handleJoinLobby(joinMessage);

        assertFalse(response.isSuccess());
        assertEquals("Lobby not found", response.getMessage());
        assertNull(response.getLobby());
    }

    @Test
    void testHandleLeaveLobby() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        CreateLobbyMessage createMessage = new CreateLobbyMessage();
        createMessage.setLobbyName("TestLobby");
        createMessage.setUserId("1");
        createMessage.setNickName("Host");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        JoinLobbyMessage joinMessage = new JoinLobbyMessage();
        joinMessage.setLobbyId(createResponse.getLobbyId());
        joinMessage.setUserId("2");
        joinMessage.setNickName("Player");

        controller.handleJoinLobby(joinMessage);

        LeaveLobbyMessage leaveMessage = new LeaveLobbyMessage();
        leaveMessage.setLobbyId(createResponse.getLobbyId());
        leaveMessage.setUserId("2");

        LobbyResponse leaveResponse = controller.handleLeaveLobby(leaveMessage);

        assertTrue(leaveResponse.isSuccess());
        assertEquals("Left lobby", leaveResponse.getMessage());
        assertNotNull(leaveResponse.getLobby());
        assertEquals(1, leaveResponse.getLobby().getUsers().size());
    }

    @Test
    void testHandleLeaveLobbyDeletesEmptyLobby() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        CreateLobbyMessage createMessage = new CreateLobbyMessage();
        createMessage.setLobbyName("TestLobby");
        createMessage.setUserId("1");
        createMessage.setNickName("Host");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        LeaveLobbyMessage leaveMessage = new LeaveLobbyMessage();
        leaveMessage.setLobbyId(createResponse.getLobbyId());
        leaveMessage.setUserId("1");

        LobbyResponse leaveResponse = controller.handleLeaveLobby(leaveMessage);

        assertTrue(leaveResponse.isSuccess());
        assertEquals("Lobby deleted (empty)", leaveResponse.getMessage());
        assertEquals(createResponse.getLobbyId(), leaveResponse.getLobbyId());
        assertNull(leaveResponse.getLobby());
    }

    @Test
    void testHandleDeleteLobby() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        CreateLobbyMessage createMessage = new CreateLobbyMessage();
        createMessage.setLobbyName("TestLobby");
        createMessage.setUserId("1");
        createMessage.setNickName("Host");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        DeleteLobbyMessage deleteMessage = new DeleteLobbyMessage();
        deleteMessage.setLobbyId(createResponse.getLobbyId());
        deleteMessage.setRequesterId("1");

        LobbyResponse deleteResponse = controller.handleDeleteLobby(deleteMessage);

        assertTrue(deleteResponse.isSuccess());
        assertEquals("Lobby deleted", deleteResponse.getMessage());
        assertEquals(createResponse.getLobbyId(), deleteResponse.getLobbyId());
        assertNull(deleteResponse.getLobby());
    }

    @Test
    void testHandleDeleteLobbyFailsForNonHost() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        CreateLobbyMessage createMessage = new CreateLobbyMessage();
        createMessage.setLobbyName("TestLobby");
        createMessage.setUserId("1");
        createMessage.setNickName("Host");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        DeleteLobbyMessage deleteMessage = new DeleteLobbyMessage();
        deleteMessage.setLobbyId(createResponse.getLobbyId());
        deleteMessage.setRequesterId("2");

        LobbyResponse deleteResponse = controller.handleDeleteLobby(deleteMessage);

        assertFalse(deleteResponse.isSuccess());
        assertEquals("Only host can delete lobby", deleteResponse.getMessage());
        assertNull(deleteResponse.getLobby());
    }

    @Test
    void testHandleCreateLobbyException() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        LobbyResponse response = controller.handleCreateLobby(null);

        assertFalse(response.isSuccess());
        assertNull(response.getLobby());
    }

    @Test
    void testHandleJoinLobbyException() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        LobbyResponse response = controller.handleJoinLobby(null);

        assertFalse(response.isSuccess());
        assertNull(response.getLobby());
    }

    @Test
    void testHandleLeaveLobbyException() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        LobbyResponse response = controller.handleLeaveLobby(null);

        assertFalse(response.isSuccess());
        assertNull(response.getLobby());
    }

    @Test
    void testHandleDeleteLobbyException() {
        WebSocketBrokerController controller = new WebSocketBrokerController();
        LobbyResponse response = controller.handleDeleteLobby(null);

        assertFalse(response.isSuccess());
        assertNull(response.getLobby());
    }

    // --- handleStartPositionRequest ---

    private WebSocketBrokerController controllerWithMockTemplate(SimpMessagingTemplate template) {
        return new WebSocketBrokerController(template);
    }

    @Test
    void testHandleStartPositionRequest_GameNotFound() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest("unknown-game", "player-1");
        controller.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/unknown-game/player/player-1/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertEquals("Game not found", response.getMessage());
        assertNull(response.getStartPosition());
    }

    @Test
    void testHandleStartPositionRequest_InvalidPlayer() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = controllerWithMockTemplate(template);

        // register a real game with no players
        GameState gameState = new GameState("game-xyz");
        GameController.getInstance().addGame("game-xyz", gameState);

        StartPositionRequest request = new StartPositionRequest("game-xyz", "unknown-player");
        controller.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/game-xyz/player/unknown-player/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertNotNull(response.getMessage());
        assertNull(response.getStartPosition());

        GameController.getInstance().removeGame("game-xyz");
    }

    @Test
    void testHandleStartPositionRequest_Success() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = controllerWithMockTemplate(template);

        // set up a real game with a player
        GameState gameState = new GameState("game-abc");
        Lobby mockLobby = mock(Lobby.class);
        User player = new User("player-1", "TestPlayer");
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(player));
        when(mockLobby.getSelectedRole("player-1")).thenReturn(Role.DETECTIVE);
        gameState.initializeFromLobby(mockLobby);
        GameController.getInstance().addGame("game-abc", gameState);

        StartPositionRequest request = new StartPositionRequest("game-abc", "player-1");
        controller.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/game-abc/player/player-1/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("START_POSITION_ASSIGNED", response.getType());
        assertEquals("game-abc", response.getGameId());
        assertEquals("player-1", response.getPlayerId());
        assertNotNull(response.getStartPosition());
        assertTrue(response.getStartPosition() >= 1 && response.getStartPosition() <= 199);

        // calling again returns the same position
        controller.handleStartPositionRequest(request);
        ArgumentCaptor<Object> captor2 = ArgumentCaptor.forClass(Object.class);
        verify(template, times(2)).convertAndSend(
                eq("/topic/game/game-abc/player/player-1/start-position"),
                captor2.capture()
        );
        StartPositionResponse response2 = (StartPositionResponse) captor2.getAllValues().get(1);
        assertEquals(response.getStartPosition(), response2.getStartPosition());

        GameController.getInstance().removeGame("game-abc");
    }
}