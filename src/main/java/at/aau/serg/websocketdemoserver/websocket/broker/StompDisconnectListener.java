package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.mapper.GameStateMapper;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.PlayerSessionService;
import at.aau.serg.websocketdemoserver.service.SessionAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for STOMP session disconnects. When a user disconnects:
 * 1. Mark the user as disconnected in SessionAuthService
 * 2. If the user is in a running game, broadcast the updated GameState
 *    so other players (especially the host) see the disconnect.
 */
@Component
public class StompDisconnectListener implements ApplicationListener<SessionDisconnectEvent> {

    private static final Logger log = LoggerFactory.getLogger(StompDisconnectListener.class);

    private final SessionAuthService sessionAuthService;
    private final PlayerSessionService playerSessionService;
    private final GameController gameController;
    private final SimpMessagingTemplate messagingTemplate;

    public StompDisconnectListener(SessionAuthService sessionAuthService,
                                   PlayerSessionService playerSessionService,
                                   GameController gameController,
                                   SimpMessagingTemplate messagingTemplate) {
        this.sessionAuthService = sessionAuthService;
        this.playerSessionService = playerSessionService;
        this.gameController = gameController;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sessionAuthService.unbindSession(sessionId);
        if (userId == null) {
            return;
        }
        log.info("User {} disconnected (session={})", userId, sessionId);

        // If user is in a running game, broadcast updated GameState
        String gameId = playerSessionService.getGameForPlayer(userId);
        if (gameId != null) {
            GameState gameState = gameController.getGame(gameId);
            if (gameState != null) {
                GameStateDto dto = GameStateMapper.toDto(gameState);
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/movements", dto);
            }
        }
    }
}