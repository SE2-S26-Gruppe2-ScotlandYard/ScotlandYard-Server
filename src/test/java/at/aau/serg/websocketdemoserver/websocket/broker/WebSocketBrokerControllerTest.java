package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.dtos.game.StartPositionConfirmRequest;
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
    void testHandleUserConnect_duplicateNickname_returnsExistingUser() {
        UserConnectMessage first = new UserConnectMessage();
        first.setNickName("dupetest");
        controller.handleUserConnect(first);

        UserConnectMessage second = new UserConnectMessage();
        second.setNickName(first.getNickName());
        UserConnectResponse response = controller.handleUserConnect(second);
        assertTrue(response.isSuccess());
        assertEquals("dupetest", response.getUser().nickName());
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

        // GameState must be broadcast so board renders the figure
        verify(template).convertAndSend(
                eq("/topic/game/game-abc/movements"),
                any(GameStateDto.class)
        );

        // calling again returns the same position
        localController.handleStartPositionRequest(request);
        ArgumentCaptor<Object> captor2 = ArgumentCaptor.forClass(Object.class);
        verify(template, times(2)).convertAndSend(
                eq("/topic/game/game-abc/player/player-1/start-position"),
                captor2.capture()
        );
        StartPositionResponse response2 = (StartPositionResponse) captor2.getAllValues().get(1);
        assertEquals(response.getStartPosition(), response2.getStartPosition());

        // Two successful calls → two broadcasts
        verify(template, times(2)).convertAndSend(
                eq("/topic/game/game-abc/movements"),
                any(GameStateDto.class)
        );

        GameController.getInstance().removeGame("game-abc");
    }

    // ── handleStartPositionRequest – cheat/manual position ────────────────

    private GameState buildGameWithPlayer(String gameId, String playerId) {
        GameState gs = new GameState(gameId);
        Lobby lobby = mock(Lobby.class);
        User player = new User(playerId, "TestPlayer");
        when(lobby.canStartGame()).thenReturn(true);
        when(lobby.getUsers()).thenReturn(List.of(player));
        when(lobby.getSelectedRole(playerId)).thenReturn(Role.DETECTIVE);
        gs.initializePlayersFromLobby(lobby);
        GameController.getInstance().addGame(gameId, gs);
        return gs;
    }

    @Test
    void testHandleStartPositionRequest_validSelectedPosition_usesIt() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("cheat-game-1", "player-cheat");

        StartPositionRequest request = new StartPositionRequest("cheat-game-1", "player-cheat", 77);
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-1/player/player-cheat/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("START_POSITION_ASSIGNED", response.getType());
        assertEquals(77, response.getStartPosition());

        // The updated GameState must also be broadcast so the board renders the new figure
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-1/movements"),
                any(GameStateDto.class)
        );

        GameController.getInstance().removeGame("cheat-game-1");
    }

    @Test
    void testHandleStartPositionRequest_selectedPositionBelowRange_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("cheat-game-2", "player-cheat");

        StartPositionRequest request = new StartPositionRequest("cheat-game-2", "player-cheat", 0);
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-2/player/player-cheat/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertNull(response.getStartPosition());

        GameController.getInstance().removeGame("cheat-game-2");
    }

    @Test
    void testHandleStartPositionRequest_selectedPositionAboveRange_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("cheat-game-3", "player-cheat");

        StartPositionRequest request = new StartPositionRequest("cheat-game-3", "player-cheat", 200);
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-3/player/player-cheat/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertNull(response.getStartPosition());

        GameController.getInstance().removeGame("cheat-game-3");
    }

    @Test
    void testHandleStartPositionRequest_selectedPositionAlreadyTaken_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        String gameId = "cheat-game-4";
        GameState gs = new GameState(gameId);
        Lobby lobby = mock(Lobby.class);
        User p1 = new User("p1", "Player1");
        User p2 = new User("p2", "Player2");
        when(lobby.canStartGame()).thenReturn(true);
        when(lobby.getUsers()).thenReturn(List.of(p1, p2));
        when(lobby.getSelectedRole("p1")).thenReturn(Role.DETECTIVE);
        when(lobby.getSelectedRole("p2")).thenReturn(Role.DETECTIVE);
        gs.initializePlayersFromLobby(lobby);
        GameController.getInstance().addGame(gameId, gs);

        // p1 takes position 55
        localController.handleStartPositionRequest(new StartPositionRequest(gameId, "p1", 55));
        // p1 success → one broadcast to movements
        verify(template, times(1)).convertAndSend(
                eq("/topic/game/" + gameId + "/movements"),
                any(GameStateDto.class)
        );

        // p2 tries to take the same position
        localController.handleStartPositionRequest(new StartPositionRequest(gameId, "p2", 55));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template, times(1)).convertAndSend(
                eq("/topic/game/" + gameId + "/player/p2/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertNull(response.getStartPosition());

        GameController.getInstance().removeGame(gameId);
    }

    @Test
    void testHandleStartPositionRequest_noSelectedPosition_fallsBackToRandom() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("cheat-game-5", "player-auto");

        // no selectedStartPosition field → backward-compat constructor
        StartPositionRequest request = new StartPositionRequest("cheat-game-5", "player-auto");
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-5/player/player-auto/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("START_POSITION_ASSIGNED", response.getType());
        assertNotNull(response.getStartPosition());
        assertTrue(response.getStartPosition() >= 1 && response.getStartPosition() <= 199);

        // GameState broadcast must also happen on random-fallback path
        verify(template).convertAndSend(
                eq("/topic/game/cheat-game-5/movements"),
                any(GameStateDto.class)
        );

        GameController.getInstance().removeGame("cheat-game-5");
    }

    // ── handleStartPositionRequest – null / blank input guards ───────────────

    @Test
    void testHandleStartPositionRequest_nullRequest_doesNotThrow() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        assertDoesNotThrow(() -> localController.handleStartPositionRequest(null));
        // nothing should be sent – no recipient address available
        verifyNoInteractions(template);
    }

    @Test
    void testHandleStartPositionRequest_nullGameId_sendsErrorToGenericTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest(null, "player-1");
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/game/error"), captor.capture());

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertEquals("gameId must not be blank", response.getMessage());
    }

    @Test
    void testHandleStartPositionRequest_blankGameId_sendsErrorToGenericTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest("  ", "player-1");
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/game/error"), captor.capture());

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertEquals("gameId must not be blank", response.getMessage());
    }

    @Test
    void testHandleStartPositionRequest_nullPlayerId_sendsErrorToGameTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest("some-game", null);
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/some-game/player/unknown/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertEquals("playerId must not be blank", response.getMessage());
    }

    @Test
    void testHandleStartPositionRequest_blankPlayerId_sendsErrorToGameTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        StartPositionRequest request = new StartPositionRequest("some-game", "");
        localController.handleStartPositionRequest(request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/some-game/player/unknown/start-position"),
                captor.capture()
        );

        StartPositionResponse response = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", response.getType());
        assertEquals("playerId must not be blank", response.getMessage());
    }

    // ── handleConfirmStartPosition ────────────────────────────────────────────

    @Test
    void testHandleConfirmStartPosition_validPosition_sendsAckToPlayerTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("conf-game-1", "player-conf");

        StartPositionConfirmRequest req = new StartPositionConfirmRequest("conf-game-1", "player-conf", 55);
        localController.handleConfirmStartPosition(req);

        // ONLY the player-specific topic must receive the ack – no board-state broadcast
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/conf-game-1/player/player-conf/start-position"),
                captor.capture()
        );
        StartPositionResponse ack = (StartPositionResponse) captor.getValue();
        assertEquals("START_POSITION_CONFIRMED", ack.getType());
        assertEquals(55, ack.getStartPosition());

        // Must NOT broadcast GameStateDto to the movements topic
        verify(template, never()).convertAndSend(
                eq("/topic/game/conf-game-1/movements"),
                any(GameStateDto.class)
        );

        GameController.getInstance().removeGame("conf-game-1");
    }

    @Test
    void testHandleConfirmStartPosition_positionBelowRange_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("conf-game-2", "pc2");

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest("conf-game-2", "pc2", 0));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/conf-game-2/player/pc2/start-position"),
                captor.capture()
        );
        StartPositionResponse resp = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", resp.getType());

        GameController.getInstance().removeGame("conf-game-2");
    }

    @Test
    void testHandleConfirmStartPosition_positionAboveRange_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        buildGameWithPlayer("conf-game-3", "pc3");

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest("conf-game-3", "pc3", 200));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/conf-game-3/player/pc3/start-position"),
                captor.capture()
        );
        StartPositionResponse resp = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", resp.getType());

        GameController.getInstance().removeGame("conf-game-3");
    }

    @Test
    void testHandleConfirmStartPosition_gameNotFound_returnsError() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        localController.handleConfirmStartPosition(
                new StartPositionConfirmRequest("no-such-game", "player-x", 42));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/no-such-game/player/player-x/start-position"),
                captor.capture()
        );
        StartPositionResponse resp = (StartPositionResponse) captor.getValue();
        assertEquals("ERROR", resp.getType());
    }

    @Test
    void testHandleConfirmStartPosition_positionAlreadyTaken_assignsFallbackPosition() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        String gameId = "conf-game-4";
        GameState gs = new GameState(gameId);
        Lobby lobby = mock(Lobby.class);
        User p1 = new User("cp1", "Conf1");
        User p2 = new User("cp2", "Conf2");
        when(lobby.canStartGame()).thenReturn(true);
        when(lobby.getUsers()).thenReturn(List.of(p1, p2));
        when(lobby.getSelectedRole("cp1")).thenReturn(Role.DETECTIVE);
        when(lobby.getSelectedRole("cp2")).thenReturn(Role.DETECTIVE);
        gs.initializePlayersFromLobby(lobby);
        GameController.getInstance().addGame(gameId, gs);

        // first player takes position 66
        localController.handleConfirmStartPosition(new StartPositionConfirmRequest(gameId, "cp1", 66));

        // second player requests the same position → server assigns a free fallback
        localController.handleConfirmStartPosition(new StartPositionConfirmRequest(gameId, "cp2", 66));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template, times(1)).convertAndSend(
                eq("/topic/game/" + gameId + "/player/cp2/start-position"),
                captor.capture()
        );
        StartPositionResponse resp = (StartPositionResponse) captor.getValue();
        assertEquals("START_POSITION_CONFIRMED", resp.getType());
        assertNotNull(resp.getStartPosition());
        assertNotEquals(66, resp.getStartPosition());
        assertTrue(resp.getStartPosition() >= 1 && resp.getStartPosition() <= 199);

        GameController.getInstance().removeGame(gameId);
    }


    // ── handleConfirmStartPosition – null / blank input guards ───────────────

    @Test
    void testHandleConfirmStartPosition_nullRequest_doesNotThrow() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        assertDoesNotThrow(() -> localController.handleConfirmStartPosition(null));
        verifyNoInteractions(template);
    }

    @Test
    void testHandleConfirmStartPosition_nullGameId_sendsErrorToGenericTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest(null, "player-x", 42));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/game/error"), captor.capture());
        assertEquals("ERROR", ((StartPositionResponse) captor.getValue()).getType());
        assertEquals("gameId must not be blank", ((StartPositionResponse) captor.getValue()).getMessage());
    }

    @Test
    void testHandleConfirmStartPosition_blankGameId_sendsErrorToGenericTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest("  ", "player-x", 42));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/game/error"), captor.capture());
        assertEquals("ERROR", ((StartPositionResponse) captor.getValue()).getType());
    }

    @Test
    void testHandleConfirmStartPosition_nullPlayerId_sendsErrorToGameTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest("some-game", null, 42));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/some-game/player/unknown/start-position"), captor.capture());
        assertEquals("ERROR", ((StartPositionResponse) captor.getValue()).getType());
        assertEquals("playerId must not be blank", ((StartPositionResponse) captor.getValue()).getMessage());
    }

    @Test
    void testHandleConfirmStartPosition_blankPlayerId_sendsErrorToGameTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController localController = controllerWithMockTemplate(template);

        localController.handleConfirmStartPosition(new StartPositionConfirmRequest("some-game", "", 42));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(
                eq("/topic/game/some-game/player/unknown/start-position"), captor.capture());
        assertEquals("ERROR", ((StartPositionResponse) captor.getValue()).getType());
    }

    // ── BackToLobby ───────────────────────────────────────────────────────────

    @Test
    void testHandleBackToLobby_hostCanReturn() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        BackToLobbyMessage msg = new BackToLobbyMessage();
        msg.setLobbyId(lobbyId);
        msg.setRequesterId("1");
        controller.handleBackToLobby(msg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/lobby/" + lobbyId),
                argThat((LobbyResponse r) -> r.isSuccess()
                        && "Host returned to Lobby".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleBackToLobby_failsForNonHost() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        controller.handleJoinLobby(new JoinLobbyMessage(lobbyId, "2", "Player"));

        BackToLobbyMessage msg = new BackToLobbyMessage();
        msg.setLobbyId(lobbyId);
        msg.setRequesterId("2");
        controller.handleBackToLobby(msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/2"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void testHandleBackToLobby_failsWhenLobbyNotFound() {
        BackToLobbyMessage msg = new BackToLobbyMessage();
        msg.setLobbyId("missing-lobby");
        msg.setRequesterId("1");
        controller.handleBackToLobby(msg);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/player/1"),
                argThat((LobbyResponse r) -> !r.isSuccess())
        );
    }

    // ── StartGame – missing branches ──────────────────────────────────────────

    @Test
    void testHandleStartGame_failsWhenNotAllRolesSelected() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "host-1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        controller.handleJoinLobby(new JoinLobbyMessage(lobbyId, "player-2", "Player2"));
        // roles NOT set → allPlayersHaveSelectedRole() returns false

        StartGameMessage msg = new StartGameMessage(lobbyId, "host-1");
        controller.handleStartGame(msg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/player/host-1"),
                argThat((LobbyResponse r) -> !r.isSuccess()
                        && "Not all players have selected a role".equals(r.getMessage()))
        );
    }

    @Test
    void testHandleStartGame_failsWhenNoMrX() {
        CreateLobbyMessage createMsg = new CreateLobbyMessage("TestLobby", "host-1", "Host");
        controller.handleCreateLobby(createMsg);
        var captor = ArgumentCaptor.forClass(LobbyResponse.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby"), captor.capture());
        String lobbyId = captor.getValue().getLobbyId();

        controller.handleJoinLobby(new JoinLobbyMessage(lobbyId, "player-2", "Player2"));
        // both select DETECTIVE → hasExactlyOneMrX() returns false
        controller.handleSetRole(new SetRoleMessage(lobbyId, "host-1",   "host-1",   "DETECTIVE"));
        controller.handleSetRole(new SetRoleMessage(lobbyId, "player-2", "player-2", "DETECTIVE"));

        StartGameMessage msg = new StartGameMessage(lobbyId, "host-1");
        controller.handleStartGame(msg);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/player/host-1"),
                argThat((LobbyResponse r) -> !r.isSuccess()
                        && "Exactly one player must play as Mr. X".equals(r.getMessage()))
        );
    }
}