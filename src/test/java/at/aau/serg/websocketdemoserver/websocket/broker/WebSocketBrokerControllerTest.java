package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.CreateLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.DeleteLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.JoinLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LeaveLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LobbyResponse;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WebSocketBrokerControllerTest {

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
        message.setUserName("Host");
        message.setPassword("pass");

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
        createMessage.setUserName("Host");
        createMessage.setPassword("pass");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        JoinLobbyMessage joinMessage = new JoinLobbyMessage();
        joinMessage.setLobbyId(createResponse.getLobbyId());
        joinMessage.setUserId("2");
        joinMessage.setUserName("Player");
        joinMessage.setPassword("pass");

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
        joinMessage.setUserName("Player");
        joinMessage.setPassword("pass");

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
        createMessage.setUserName("Host");
        createMessage.setPassword("pass");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        JoinLobbyMessage joinMessage = new JoinLobbyMessage();
        joinMessage.setLobbyId(createResponse.getLobbyId());
        joinMessage.setUserId("2");
        joinMessage.setUserName("Player");
        joinMessage.setPassword("pass");

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
        createMessage.setUserName("Host");
        createMessage.setPassword("pass");

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
        createMessage.setUserName("Host");
        createMessage.setPassword("pass");

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
        createMessage.setUserName("Host");
        createMessage.setPassword("pass");

        LobbyResponse createResponse = controller.handleCreateLobby(createMessage);

        DeleteLobbyMessage deleteMessage = new DeleteLobbyMessage();
        deleteMessage.setLobbyId(createResponse.getLobbyId());
        deleteMessage.setRequesterId("2");

        LobbyResponse deleteResponse = controller.handleDeleteLobby(deleteMessage);

        assertFalse(deleteResponse.isSuccess());
        assertEquals("Only host can delete lobby", deleteResponse.getMessage());
        assertNull(deleteResponse.getLobby());
    }
}