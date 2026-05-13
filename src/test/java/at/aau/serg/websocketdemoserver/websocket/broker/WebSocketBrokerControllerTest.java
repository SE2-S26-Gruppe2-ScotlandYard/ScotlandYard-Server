package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.*;
import at.aau.serg.websocketdemoserver.dtos.movement.*;
import at.aau.serg.websocketdemoserver.gamelogic.*;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.lobby.*;
import at.aau.serg.websocketdemoserver.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class WebSocketBrokerControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private LobbyService lobbyService;
    private GameController gameController;
    private WebSocketBrokerController controller;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        lobbyService = mock(LobbyService.class);
        gameController = mock(GameController.class);

        controller = new WebSocketBrokerController(messagingTemplate);

        ReflectionTestUtils.setField(controller, "lobbyService", lobbyService);
        ReflectionTestUtils.setField(controller, "gameController", gameController);
    }

    private GameState setupMockGame(String gameId) {
        GameState state = spy(new GameState(gameId));
        User host = new User("host-mrx", "MrX");
        Lobby lobby = new Lobby("Test", host);
        lobby.addUser(new User("det-1", "Det"));
        lobby.addUser(new User("det-2", "Det2"));
        lobby.selectRole("host-mrx", Role.MRX);
        lobby.selectRole("det-1", Role.DETECTIVE);
        lobby.selectRole("det-2", Role.DETECTIVE);
        lobby.markPlayerReady("host-mrx");
        lobby.markPlayerReady("det-1");
        lobby.markPlayerReady("det-2");

        state.initializeFromLobby(lobby);
        when(gameController.getGame(gameId)).thenReturn(state);
        return state;
    }

    @Test
    void testHandleUserConnect() {
        UserConnectMessage msg = new UserConnectMessage();
        msg.setNickName("Stefan");
        assertNotNull(controller.handleUserConnect(msg));
    }

    @Test
    void testHandleHelloAndObject() {
        assertEquals("echo from broker: x", controller.handleHello("x"));
        StompMessage s = new StompMessage("A", "B");
        assertSame(s, controller.handleObject(s));
    }

    @Test
    void testLobbyMethodChain() {
        controller.handleCreateLobby(new CreateLobbyMessage());
        when(lobbyService.joinLobby(any(), any())).thenReturn(new Lobby("L", new User("u", "n")));
        controller.handleJoinLobby(new JoinLobbyMessage());
        controller.handleLeaveLobby(new LeaveLobbyMessage());
        controller.handleDeleteLobby(new DeleteLobbyMessage());
        controller.handleKickPlayer(new KickPlayerMessage());
        controller.handleSetRole(new SetRoleMessage());

        verify(messagingTemplate, atLeast(6)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void testHandleMove_AllErrorPaths() {
        controller.handleMove(null, new MovementMessage());
        controller.handleMove("G1", null);
        MovementMessage m1 = new MovementMessage();
        controller.handleMove("G1", m1);
        when(gameController.getGame("G1")).thenReturn(null);
        m1.setPlayerId("P1");
        controller.handleMove("G1", m1);

        verify(messagingTemplate, atLeast(4)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void testHandleMove_TurnAndPhaseValidation() {
        String gid = "G1";
        GameState state = setupMockGame(gid);

        state.getRoundController().setCurrentPhase(TurnType.MRX);
        MovementMessage msg = new MovementMessage();
        msg.setPlayerId("det-1");
        controller.handleMove(gid, msg);

        state.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        msg.setPlayerId("host-mrx");
        controller.handleMove(gid, msg);

        verify(messagingTemplate, atLeast(2)).convertAndSend(contains("/topic/player/"), (Object) any());
    }

    @Test
    void testHandleMove_DoubleMove_FullBranch() {
        String gid = "double";
        GameState state = setupMockGame(gid);
        state.getRoundController().setCurrentPhase(TurnType.MRX);

        doReturn(false).when(state).activateDoubleMove();
        MovementMessage msg = new MovementMessage();
        msg.setPlayerId("host-mrx");
        msg.setTicket(TicketType.DOUBLE);
        controller.handleMove(gid, msg);

        doReturn(true).when(state).activateDoubleMove();
        controller.handleMove(gid, msg);

        verify(messagingTemplate, atLeast(3)).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void testHandleMove_WinConditions_Coverage() {
        String gid = "win";
        GameState state = setupMockGame(gid);
        doReturn(true).when(state).movePlayer(anyString(), any(), anyInt());

        doReturn(GameResult.MRX_WINS).when(state).checkGameResult();
        state.getRoundController().setCurrentPhase(TurnType.MRX);
        MovementMessage msg = new MovementMessage();
        msg.setPlayerId("host-mrx");
        msg.setTicket(TicketType.WALKING);
        controller.handleMove(gid, msg);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/win/over"), eq("MRX_WINS"));

        doReturn(GameResult.DETECTIVES_WIN).when(state).checkGameResult();
        state.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        state.getRoundController().addPendingDetectives("det-1");
        msg.setPlayerId("det-1");
        controller.handleMove(gid, msg);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/win/over"), eq("DETECTIVES_WIN"));
    }

    @Test
    void testCatchBlocks_LobbyExceptions() {
        when(lobbyService.kickPlayer(any(), any(), any())).thenThrow(new RuntimeException("Fail"));
        KickPlayerMessage msg = new KickPlayerMessage();
        msg.setRequesterId("r1");
        controller.handleKickPlayer(msg);

        verify(messagingTemplate).convertAndSend(eq("/topic/player/r1"), (Object) any());
    }

    @Test
    void testHandleStartRoleSelection_HostValid() {
        Lobby l = new Lobby("L", new User("h1", "H"));
        when(lobbyService.getLobby(any())).thenReturn(l);
        StartRoleSelectionMessage msg = new StartRoleSelectionMessage();
        msg.setRequesterId("h1");
        controller.handleStartRoleSelection(msg);
        assertTrue(l.isLocked());
    }

    @Test
    void testHandleBackToLobby_HostValid() {
        Lobby l = new Lobby("L", new User("h1", "H"));
        l.setLocked(true);
        when(lobbyService.getLobby(any())).thenReturn(l);
        BackToLobbyMessage msg = new BackToLobbyMessage();
        msg.setRequesterId("h1");
        controller.handleBackToLobby(msg);
        assertFalse(l.isLocked());
    }
}