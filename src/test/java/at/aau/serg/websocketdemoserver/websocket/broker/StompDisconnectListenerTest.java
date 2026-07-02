package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.PlayerSessionService;
import at.aau.serg.websocketdemoserver.service.SessionAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompDisconnectListenerTest {

    private SessionAuthService sessionAuthService;
    private PlayerSessionService playerSessionService;
    private GameController gameController;
    private SimpMessagingTemplate messagingTemplate;
    private StompDisconnectListener listener;

    @BeforeEach
    void setUp() {
        sessionAuthService = mock(SessionAuthService.class);
        playerSessionService = mock(PlayerSessionService.class);
        gameController = mock(GameController.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        listener = new StompDisconnectListener(sessionAuthService, playerSessionService, gameController, messagingTemplate);
    }

    private SessionDisconnectEvent mockEvent(String sessionId) {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(sessionId);
        return event;
    }

    private GameState realGameStateWithFourPlayers() {
        GameState gameState = new GameState("game1");
        User host = new User("host1", "Host");
        User det1 = new User("det1", "Detective1");
        User det2 = new User("det2", "Detective2");
        User mrX = new User("mrx1", "MrX");

        Lobby mockLobby = mock(Lobby.class);
        when(mockLobby.getHostId()).thenReturn("host1");
        when(mockLobby.getUsers()).thenReturn(Arrays.asList(host, det1, det2, mrX));
        when(mockLobby.getSelectedRole("host1")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("det1")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("det2")).thenReturn(Role.DETECTIVE);
        when(mockLobby.getSelectedRole("mrx1")).thenReturn(Role.MRX);

        gameState.initializePlayersFromLobby(mockLobby);
        return gameState;
    }

    @Test
    void testOnApplicationEvent_unknownSession_doesNothing() {
        when(sessionAuthService.unbindSession("session1")).thenReturn(null);

        listener.onApplicationEvent(mockEvent("session1"));

        verifyNoInteractions(playerSessionService, gameController, messagingTemplate);
    }

    @Test
    void testOnApplicationEvent_userNotInAnyGame_noBroadcast() {
        when(sessionAuthService.unbindSession("session1")).thenReturn("user1");
        when(playerSessionService.getGameForPlayer("user1")).thenReturn(null);

        listener.onApplicationEvent(mockEvent("session1"));

        verifyNoInteractions(gameController, messagingTemplate);
    }

    @Test
    void testOnApplicationEvent_gameNoLongerExists_noBroadcast() {
        when(sessionAuthService.unbindSession("session1")).thenReturn("user1");
        when(playerSessionService.getGameForPlayer("user1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(null);

        listener.onApplicationEvent(mockEvent("session1"));

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void testOnApplicationEvent_broadcastsUpdatedGameStateToCorrectTopic() {
        GameState gameState = realGameStateWithFourPlayers();
        when(sessionAuthService.unbindSession("session1")).thenReturn("det1");
        when(playerSessionService.getGameForPlayer("det1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(gameState);
        when(sessionAuthService.getDisconnectedUsers()).thenReturn(Set.of("det1"));

        listener.onApplicationEvent(mockEvent("session1"));

        ArgumentCaptor<GameStateDto> dtoCaptor = ArgumentCaptor.forClass(GameStateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/game1/movements"), dtoCaptor.capture());
        assertNotNull(dtoCaptor.getValue());
    }

    @Test
    void testOnApplicationEvent_disconnectedPlayersAreIncludedInBroadcastDto() {
        // Regression test: the broadcast used to call GameStateMapper.toDto(gameState)
        // WITHOUT the disconnected-users set, so clients never saw the "offline"
        // marker on a plain disconnect. Now it must be included, consistent with
        // WebSocketBrokerController.broadcastGameState().
        GameState gameState = realGameStateWithFourPlayers();
        when(sessionAuthService.unbindSession("session1")).thenReturn("det1");
        when(playerSessionService.getGameForPlayer("det1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(gameState);
        when(sessionAuthService.getDisconnectedUsers()).thenReturn(Set.of("det1"));

        listener.onApplicationEvent(mockEvent("session1"));

        ArgumentCaptor<GameStateDto> dtoCaptor = ArgumentCaptor.forClass(GameStateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/game1/movements"), dtoCaptor.capture());
        assertTrue(dtoCaptor.getValue().getDisconnectedPlayers().contains("det1"));
    }

    @Test
    void testOnApplicationEvent_hostDisconnects_reassignsHostToAnotherConnectedPlayer() {
        GameState gameState = realGameStateWithFourPlayers();
        assertEquals("host1", gameState.getHostId());

        when(sessionAuthService.unbindSession("session1")).thenReturn("host1");
        when(playerSessionService.getGameForPlayer("host1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(gameState);
        // Only det2 remains connected once host1 has disconnected.
        when(sessionAuthService.getDisconnectedUsers()).thenReturn(Set.of("host1", "det1", "mrx1"));

        listener.onApplicationEvent(mockEvent("session1"));

        assertEquals("det2", gameState.getHostId());
        ArgumentCaptor<GameStateDto> dtoCaptor = ArgumentCaptor.forClass(GameStateDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/game1/movements"), dtoCaptor.capture());
        assertEquals("det2", dtoCaptor.getValue().getHostId());
    }

    @Test
    void testOnApplicationEvent_nonHostDisconnects_hostStaysTheSame() {
        GameState gameState = realGameStateWithFourPlayers();

        when(sessionAuthService.unbindSession("session1")).thenReturn("det1");
        when(playerSessionService.getGameForPlayer("det1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(gameState);
        when(sessionAuthService.getDisconnectedUsers()).thenReturn(Set.of("det1"));

        listener.onApplicationEvent(mockEvent("session1"));

        assertEquals("host1", gameState.getHostId());
    }

    @Test
    void testOnApplicationEvent_hostDisconnectsAsOnlyPlayer_noReassignmentButStillBroadcasts() {
        GameState gameState = realGameStateWithFourPlayers();
        gameState.kickPlayer("host1", "det1");
        gameState.kickPlayer("host1", "det2");
        gameState.kickPlayer("host1", "mrx1");

        when(sessionAuthService.unbindSession("session1")).thenReturn("host1");
        when(playerSessionService.getGameForPlayer("host1")).thenReturn("game1");
        when(gameController.getGame("game1")).thenReturn(gameState);
        when(sessionAuthService.getDisconnectedUsers()).thenReturn(Collections.singleton("host1"));

        listener.onApplicationEvent(mockEvent("session1"));

        assertEquals("host1", gameState.getHostId());
        verify(messagingTemplate).convertAndSend(eq("/topic/game/game1/movements"), any(GameStateDto.class));
    }
}