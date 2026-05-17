package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.StartPositionRequest;
import at.aau.serg.websocketdemoserver.dtos.game.StartPositionResponse;
import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.*;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import org.junit.jupiter.api.BeforeEach;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketBrokerControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private WebSocketBrokerController controller;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new WebSocketBrokerController(messagingTemplate);
    }

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

    // ── Bewegungstests (unverändert) ───────────────────────────
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

    // ── Lobby Tests (angepasste Messages) ─────────────────────
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
                        && "Host's Lobby created".equals(r.getMessage())
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
                        && "Player joined Host's Lobby".equals(r.getMessage())
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
                argThat((LobbyResponse r) -> r.isSuccess() && "Player left Host's Lobby".equals(r.getMessage()))
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
                        && "Host left Host's Lobby (Lobby is now empty)".equals(r.getMessage())
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
                argThat((LobbyResponse r) -> r.isSuccess() && "Host deleted the Lobby".equals(r.getMessage()))
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

    // ── KickPlayer ────────────────────────────────────────────
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
                        && "Player was kicked out of Host's Lobby".equals(r.getMessage())
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

    // ── SetRole ───────────────────────────────────────────────
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
                argThat((LobbyResponse r) -> r.isSuccess() && "Host selected role MRX".equals(r.getMessage()))
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

    // ── StartRoleSelection ───────────────────────────────────
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
                        && "Host started role selection".equals(r.getMessage()))
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

    // ── NEUE Tests: StartGame ──────────────────────────────────────────────

    /**
     * Creates a lobby with 2 players (host = MrX, player-2 = Detective).
     * setRole auto-marks them as ready. The GameState is initialized via
     * initializePlayersFromLobby (no strict canStartGame check).
     */
    private String createReadyLobbyAndGetId() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "host-1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = org.mockito.ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        controller.handleJoinLobby(new JoinLobbyMessage(lobbyId, "player-2", "Player2"));
        controller.handleSetRole(new SetRoleMessage(lobbyId, "host-1",   "host-1",   "MRX"));
        controller.handleSetRole(new SetRoleMessage(lobbyId, "player-2", "player-2", "DETECTIVE"));
        return lobbyId;
    }

    @Test
    void testHandleStartGame_hostCanStart() {
        String lobbyId = createReadyLobbyAndGetId();

        StartGameMessage msg = new StartGameMessage(lobbyId, "host-1");
        controller.handleStartGame(msg);

        // New architecture: broadcasts to /topic/lobby/{lobbyId}
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "GAME_STARTED".equals(r.getMessage())
                        && r.getLobby() != null
                        && lobbyId.equals(r.getLobbyId()))
        );

        // GameState must be registered so start-position assignment works
        assertNotNull(GameController.getInstance().getGame(lobbyId));
        GameController.getInstance().removeGame(lobbyId);
    }

    @Test
    void testHandleStartGame_failsForNonHost() {
        String lobbyId = createReadyLobbyAndGetId();

        StartGameMessage msg = new StartGameMessage(lobbyId, "player-2");
        controller.handleStartGame(msg);

        // Error goes to requester's personal topic
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/player/player-2"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleStartGame_failsWhenLobbyNotFound() {
        StartGameMessage msg = new StartGameMessage("nonexistent-lobby", "host-1");
        controller.handleStartGame(msg);

        // Error goes to requester's personal topic
        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/host-1"),
                argThat((LobbyResponse r) -> !r.isSuccess()
                        && "Lobby not found".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleStartGame_nullMessageDoesNotCrash() {
        // null message is silently ignored (no sender to respond to)
        assertDoesNotThrow(() -> controller.handleStartGame(null));
    }

    // --- handleStartPositionRequest ---

    private WebSocketBrokerController controllerWithMockTemplate(SimpMessagingTemplate template) {
        return new WebSocketBrokerController(template);
    }

    @Test
    void testHandleStartPositionRequest_GameNotFound() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest("unknown-game", "player-1");
        localController.handleStartPositionRequest(request);

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
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        // register a real game with no players
        GameState gameState = new GameState("game-xyz");
        GameController.getInstance().addGame("game-xyz", gameState);

        StartPositionRequest request = new StartPositionRequest("game-xyz", "unknown-player");
        localController.handleStartPositionRequest(request);

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
    void testHandleUserConnect_emptyNickname_returnsError() {
        UserConnectMessage message = new UserConnectMessage();
        message.setNickName("");
        UserConnectResponse response = controller.handleUserConnect(message);
        assertFalse(response.isSuccess());
        assertEquals("Nickname cannot be empty", response.getMessage());
        assertNull(response.getUser());
    }

    @Test
    void testHandleUserConnect_nullNickname_returnsError() {
        UserConnectMessage message = new UserConnectMessage();
        message.setNickName(null);
        UserConnectResponse response = controller.handleUserConnect(message);
        assertFalse(response.isSuccess());
        assertNull(response.getUser());
    }

    @Test
    void testHandleUserConnect_duplicateNickname_returnsError() {
        UserConnectMessage first = new UserConnectMessage();
        first.setNickName("UniqueUser_" + System.nanoTime());
        controller.handleUserConnect(first);

        UserConnectMessage second = new UserConnectMessage();
        second.setNickName(first.getNickName());
        UserConnectResponse response = controller.handleUserConnect(second);
        assertFalse(response.isSuccess());
        assertEquals("Nickname already taken", response.getMessage());
    }

    @Test
    void testHandleGetGameState_existingGame_broadcastsState() {
        String gameId = "state-test-game";
        GameState gameState = new GameState(gameId);
        GameController.getInstance().addGame(gameId, gameState);

        controller.handleGetGameState(gameId);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/game/" + gameId + "/movements"),
                Optional.ofNullable(any())
        );

        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleGetGameState_nonExistingGame_doesNotBroadcast() {
        controller.handleGetGameState("non-existing-game-id");

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/game/non-existing-game-id/movements"),
                Optional.ofNullable(any())
        );
    }

    private GameState setupGameWithPlayers(String gameId, String mrXId, String detectiveId) {
        GameState gameState = new GameState(gameId);
        Lobby mockLobby = mock(Lobby.class);
        User mrXUser = new User(mrXId, "MrX");
        User detectiveUser = new User(detectiveId, "Det");
        when(mockLobby.getUsers()).thenReturn(List.of(mrXUser, detectiveUser));
        when(mockLobby.getSelectedRole(mrXId)).thenReturn(Role.MRX);
        when(mockLobby.getSelectedRole(detectiveId)).thenReturn(Role.DETECTIVE);
        gameState.initializePlayersFromLobby(mockLobby);
        gameState.setPlayerPosition(mrXId, 1);
        gameState.setPlayerPosition(detectiveId, 50);
        GameController.getInstance().addGame(gameId, gameState);
        return gameState;
    }

    @Test
    void testHandleMove_playerNotFound_sendsError() {
        String gameId = "move-noplayer-game";
        GameState gameState = new GameState(gameId);
        GameController.getInstance().addGame(gameId, gameState);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId("ghost-player");
        msg.setTicket(TicketType.WALKING);
        msg.setTargetPosition(5);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/ghost-player"),
                argThat((MovementResponse r) -> !r.isSuccess())
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleMove_detectiveMovesDuringMrXPhase_sendsError() {
        String gameId = "move-wrongturn-game";
        String mrXId = "mrx-wt";
        String detId = "det-wt";
        GameState gs = setupGameWithPlayers(gameId, mrXId, detId);

        gs.getRoundController().setCurrentPhase(TurnType.MRX);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId(detId);
        msg.setTicket(TicketType.WALKING);
        msg.setTargetPosition(51);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/" + detId),
                argThat((MovementResponse r) -> !r.isSuccess()
                        && "Not the detectives' turn".equals(r.getMessage()))
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleMove_mrXMovesDuringDetectivePhase_sendsError() {
        String gameId = "move-mrx-wrongturn";
        String mrXId = "mrx-dwt";
        String detId = "det-dwt";
        GameState gs = setupGameWithPlayers(gameId, mrXId, detId);
        gs.getRoundController().setCurrentPhase(TurnType.DETECTIVES);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId(mrXId);
        msg.setTicket(TicketType.WALKING);
        msg.setTargetPosition(2);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/" + mrXId),
                argThat((MovementResponse r) -> !r.isSuccess()
                        && "Not Mr. X's turn".equals(r.getMessage()))
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleMove_detectiveAlreadyMoved_sendsError() {
        String gameId = "move-already-moved";
        String mrXId = "mrx-am";
        String detId = "det-am";
        GameState gs = setupGameWithPlayers(gameId, mrXId, detId);
        gs.getRoundController().setCurrentPhase(TurnType.DETECTIVES);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId(detId);
        msg.setTicket(TicketType.WALKING);
        msg.setTargetPosition(51);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/" + detId),
                argThat((MovementResponse r) -> !r.isSuccess()
                        && "Detective has already moved this round".equals(r.getMessage()))
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleMove_detectiveUsesDoubleTicket_sendsError() {
        String gameId = "move-det-double";
        String mrXId = "mrx-dd";
        String detId = "det-dd";
        GameState gs = setupGameWithPlayers(gameId, mrXId, detId);
        gs.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gs.getRoundController().addPendingDetectives(detId);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId(detId);
        msg.setTicket(TicketType.DOUBLE);
        msg.setTargetPosition(51);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/" + detId),
                argThat((MovementResponse r) -> !r.isSuccess()
                        && "Only Mr. X can use the DOUBLE ticket".equals(r.getMessage()))
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleMove_mrXActivatesDoubleTicket_success() {
        String gameId = "move-mrx-double";
        String mrXId = "mrx-double";
        String detId = "det-double";
        GameState gs = setupGameWithPlayers(gameId, mrXId, detId);
        gs.getRoundController().setCurrentPhase(TurnType.MRX);

        MovementMessage msg = new MovementMessage();
        msg.setGameId(gameId);
        msg.setPlayerId(mrXId);
        msg.setTicket(TicketType.DOUBLE);
        msg.setTargetPosition(0);
        controller.handleMove(gameId, msg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                any(String.class),
                any(Object.class)
        );
        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleStartPositionRequest_Success() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        // set up a real game with a player
        GameState gameState = new GameState("game-abc");
        Lobby mockLobby = mock(Lobby.class);
        User player = new User("player-1", "TestPlayer");
        when(mockLobby.canStartGame()).thenReturn(true);
        when(mockLobby.getUsers()).thenReturn(List.of(player));
        when(mockLobby.getSelectedRole("player-1")).thenReturn(Role.DETECTIVE);
        gameState.initializePlayersFromLobby(mockLobby);
        GameController.getInstance().addGame("game-abc", gameState);

        StartPositionRequest request = new StartPositionRequest("game-abc", "player-1");
        localController.handleStartPositionRequest(request);

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
        localController.handleStartPositionRequest(request);
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