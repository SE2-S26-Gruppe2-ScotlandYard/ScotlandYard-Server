package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.*;
import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.*;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.player.Player;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.mapper.GameStateMapper;
import at.aau.serg.websocketdemoserver.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketBrokerController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBrokerController.class);
    private static final String TOPIC_GAME = "/topic/game/";
    private static final String ERROR_TYPE = "ERROR";
    private static final String UNAUTHORIZED_MSG = "Unauthorized: You can only act on your own behalf";

    private final GameController gameController;
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final PlayerSessionService playerSessionService;
    private final SessionAuthService sessionAuthService;

    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate) {
        this(messagingTemplate, GameController.getInstance(), new LobbyService(), new UserService(), new PlayerSessionService(), new SessionAuthService());
    }

    @Autowired
    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate,
                                     GameController gameController,
                                     LobbyService lobbyService,
                                     UserService userService,
                                     PlayerSessionService playerSessionService,
                                     SessionAuthService sessionAuthService) {
        this.messagingTemplate = messagingTemplate;
        this.gameController = gameController;
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.playerSessionService = playerSessionService;
        this.sessionAuthService = sessionAuthService;
    }

    // ── Messaging helpers ─────────────────────────────────────────────────────

    private void sendToUser(String userId, Object payload) {
        messagingTemplate.convertAndSend("/topic/player/" + userId, payload);
    }

    private void broadcastToGlobalLobbyList(LobbyResponse response) {
        messagingTemplate.convertAndSend("/topic/lobby", response);
    }

    private void broadcastToSpecificLobby(String lobbyId, LobbyResponse response) {
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, response);
    }

    private void sendMoveResponse(String gameId, MovementResponse response) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/move-response", response);
    }

    private void broadcastGameState(String gameId, GameState gameState) {
        GameStateDto dto = GameStateMapper.toDto(gameState, sessionAuthService.getDisconnectedUsers());
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/movements", dto);
    }

    private void broadcastGameOver(String gameId, String result) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/over", result);
    }

    /**
     * Returns true and sends a lobby error response if session is not authorized for the claimed userId.
     * Returns false if session is authorized (caller should proceed).
     */
    private boolean denyIfUnauthorized(String sessionId, String claimedUserId, String lobbyIdForResponse) {
        if (!sessionAuthService.isAuthorized(sessionId, claimedUserId)) {
            log.warn("Unauthorized call: session={} tried to act as user={}", sessionId, claimedUserId);
            sendToUser(claimedUserId, new LobbyResponse(false, UNAUTHORIZED_MSG, lobbyIdForResponse, null));
            return true;
        }
        return false;
    }

    /**
     * Same as denyIfUnauthorized but sends a movement response (for game-related endpoints).
     */
    private boolean denyIfUnauthorizedGame(String sessionId, String claimedUserId) {
        if (!sessionAuthService.isAuthorized(sessionId, claimedUserId)) {
            log.warn("Unauthorized game call: session={} tried to act as user={}", sessionId, claimedUserId);
            sendToUser(claimedUserId, new MovementResponse(false, UNAUTHORIZED_MSG, 0, null));
            return true;
        }
        return false;
    }

    private boolean validateStartPositionIds(String gameId, String playerId) {
        if (gameId == null || gameId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/game/error",
                    new StartPositionResponse(ERROR_TYPE, null, null, null, "gameId must not be blank"));
            return false;
        }
        if (playerId == null || playerId.isBlank()) {
            messagingTemplate.convertAndSend(TOPIC_GAME + gameId + "/player/unknown/start-position",
                    new StartPositionResponse(ERROR_TYPE, gameId, "unknown", null, "playerId must not be blank"));
            return false;
        }
        return true;
    }

    private GameState resolveGameState(String gameId, String playerId, String playerTopic) {
        GameState gameState = gameController.getGame(gameId);
        if (gameState == null) {
            messagingTemplate.convertAndSend(playerTopic,
                    new StartPositionResponse(ERROR_TYPE, gameId, playerId, null, "Game not found"));
        }
        return gameState;
    }

    // ── Basic endpoints ───────────────────────────────────────────────────────

    @MessageMapping("/hello")
    @SendTo("/topic/hello-response")
    public String handleHello(String text) {
        return "echo from broker: " + text;
    }

    @MessageMapping("/object")
    @SendTo("/topic/rcv-object")
    public StompMessage handleObject(StompMessage msg) {
        return msg;
    }

    @MessageMapping("/user/connect")
    @SendToUser("/topic/user-response")
    public UserConnectResponse handleUserConnect(UserConnectMessage message,
                                                 @Header(value = "simpSessionId", required = false) String sessionId) {
        try {
            User user = userService.registerUser(message.getNickName());
            boolean isReconnect = message.getUserId() != null && message.getUserId().equals(user.id());
            if (!isReconnect) {
                String existingSession = sessionAuthService.getSessionForUser(user.id());
                if (existingSession != null && !existingSession.equals(sessionId)) {
                    return new UserConnectResponse(false, "Nickname already taken", null);
                }
            }
            sessionAuthService.bindSession(sessionId, user.id());
            return new UserConnectResponse(true, "User registered", user);
        } catch (IllegalArgumentException e) {
            return new UserConnectResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new UserConnectResponse(false, "Internal Server Error", null);
        }
    }
    // ── Lobby endpoints ───────────────────────────────────────────────────────

    @MessageMapping("/lobby/create")
    public void handleCreateLobby(CreateLobbyMessage message) {
        handleCreateLobby(message, null);
    }

    public void handleCreateLobby(CreateLobbyMessage message,
                                  @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getUserId(), null)) return;
        try {
            User host = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.createLobby(message.getLobbyName(), host);
            playerSessionService.playerJoinedLobby(message.getUserId(), lobby.getId());
            LobbyResponse response = new LobbyResponse(true, message.getNickName() + "'s Lobby created", lobby.getId(), lobby);
            sendToUser(message.getUserId(), response);
            broadcastToGlobalLobbyList(response);
        } catch (Exception e) {
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), null, null));
        }
    }

    @MessageMapping("/lobby/join")
    public void handleJoinLobby(JoinLobbyMessage message) {
        handleJoinLobby(message, null);
    }

    public void handleJoinLobby(JoinLobbyMessage message,
                                @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getUserId(), message.getLobbyId())) return;
        try {
            User user = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.joinLobby(message.getLobbyId(), user);
            playerSessionService.playerJoinedLobby(message.getUserId(), lobby.getId());
            String hostName = lobby.getHost().nickName();
            String responseMessage = message.getNickName() + " joined " + hostName + "'s Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            sendToUser(message.getUserId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/leave")
    public void handleLeaveLobby(LeaveLobbyMessage message) {
        handleLeaveLobby(message, null);
    }

    public void handleLeaveLobby(LeaveLobbyMessage message,
                                 @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getUserId(), message.getLobbyId())) return;
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            User leavingUser = (lobbyBefore != null) ? lobbyBefore.getUsers().stream()
                    .filter(u -> u.id().equals(message.getUserId()))
                    .findFirst().orElse(null) : null;
            String leavingUserName = (leavingUser != null) ? leavingUser.nickName() : "Unknown";
            String hostNameBefore = (lobbyBefore != null && lobbyBefore.getHost() != null)
                    ? lobbyBefore.getHost().nickName() : "Unknown";

            lobbyService.leaveLobby(message.getLobbyId(), message.getUserId());
            playerSessionService.playerFullyLeft(message.getUserId());
            Lobby updatedLobby = lobbyService.getLobby(message.getLobbyId());

            if (updatedLobby == null) {
                String responseMessage = leavingUserName + " left " + hostNameBefore + "'s Lobby (Lobby is now empty)";
                LobbyResponse response = new LobbyResponse(true, responseMessage, message.getLobbyId(), null);
                sendToUser(message.getUserId(), response);
                broadcastToGlobalLobbyList(response);
            } else {
                String hostNameAfter = updatedLobby.getHost().nickName();
                String responseMessage = leavingUserName + " left " + hostNameAfter + "'s Lobby";
                LobbyResponse response = new LobbyResponse(true, responseMessage, updatedLobby.getId(), updatedLobby);
                sendToUser(message.getUserId(), response);
                broadcastToSpecificLobby(updatedLobby.getId(), response);
            }
        } catch (Exception e) {
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/delete")
    public void handleDeleteLobby(DeleteLobbyMessage message) {
        handleDeleteLobby(message, null);
    }

    public void handleDeleteLobby(DeleteLobbyMessage message,
                                  @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            String hostName = (lobbyBefore != null && lobbyBefore.getHost() != null)
                    ? lobbyBefore.getHost().nickName() : "Unknown";
            lobbyService.deleteLobby(message.getLobbyId(), message.getRequesterId());
            playerSessionService.playerFullyLeft(message.getRequesterId());
            String responseMessage = hostName + " deleted the Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, message.getLobbyId(), null);
            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(message.getLobbyId(), response);
            broadcastToGlobalLobbyList(response);
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/kick")
    public void handleKickPlayer(KickPlayerMessage message) {
        handleKickPlayer(message, null);
    }

    public void handleKickPlayer(KickPlayerMessage message,
                                 @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            User targetUser = lobbyBefore.getUsers().stream()
                    .filter(u -> u.id().equals(message.getTargetUserId()))
                    .findFirst().orElse(null);
            String targetNickName = (targetUser != null) ? targetUser.nickName() : "Unknown";
            Lobby lobby = lobbyService.kickPlayer(
                    message.getLobbyId(),
                    message.getRequesterId(),
                    message.getTargetUserId()
            );
            playerSessionService.playerFullyLeft(message.getTargetUserId());
            String hostName = lobby.getHost().nickName();
            String responseMessage = targetNickName + " was kicked out of " + hostName + "'s Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            sendToUser(message.getTargetUserId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/setRole")
    public void handleSetRole(SetRoleMessage message) {
        handleSetRole(message, null);
    }

    public void handleSetRole(SetRoleMessage message,
                              @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobby = lobbyService.setRole(
                    message.getLobbyId(),
                    message.getRequesterId(),
                    message.getTargetUserId(),
                    message.getRole()
            );
            User targetUser = lobby.getUsers().stream()
                    .filter(u -> u.id().equals(message.getTargetUserId()))
                    .findFirst().orElse(null);
            String targetNickName = (targetUser != null) ? targetUser.nickName() : "Unknown";
            String responseMessage = targetNickName + " selected role " + message.getRole();
            broadcastToSpecificLobby(lobby.getId(), new LobbyResponse(true, responseMessage, lobby.getId(), lobby));
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/startRoleSelection")
    public void handleStartRoleSelection(StartRoleSelectionMessage message) {
        handleStartRoleSelection(message, null);
    }

    public void handleStartRoleSelection(StartRoleSelectionMessage message,
                                         @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can start role selection");
            lobby.setLocked(true);
            String hostName = lobby.getHost().nickName();
            String responseMessage = hostName + " started role selection";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/startGame")
    public void handleStartGame(StartGameMessage message) {
        handleStartGame(message, null);
    }

    public void handleStartGame(StartGameMessage message,
                                @Header(value = "simpSessionId", required = false) String sessionId) {
        if (message == null) return;
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can start the game");
            if (!lobby.allPlayersHaveSelectedRole())
                throw new IllegalStateException("Not all players have selected a role");
            if (!lobby.hasExactlyOneMrX())
                throw new IllegalStateException("Exactly one player must play as Mr. X");

            GameState gameState = new GameState(lobby.getId());
            gameState.initializePlayersFromLobby(lobby);
            gameController.addGame(lobby.getId(), gameState);

            lobby.getUsers().forEach(u ->
                    playerSessionService.playerJoinedGame(u.id(), lobby.getId()));

            broadcastToSpecificLobby(lobby.getId(), new LobbyResponse(true, "GAME_STARTED", lobby.getId(), lobby));
            broadcastGameState(lobby.getId(), gameState);
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/backToLobby")
    public void handleBackToLobby(BackToLobbyMessage message) {
        handleBackToLobby(message, null);
    }

    public void handleBackToLobby(BackToLobbyMessage message,
                                  @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getRequesterId(), message.getLobbyId())) return;
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can go back to lobby");
            lobby.setLocked(false);
            String hostName = lobby.getHost().nickName();
            String responseMessage = hostName + " returned to Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    // ── Game State Recovery endpoints ─────────────────────────────────────────

    @MessageMapping("/lobby/rejoin")
    public void handleRejoinLobby(RejoinLobbyMessage message) {
        handleRejoinLobby(message, null);
    }

    public void handleRejoinLobby(RejoinLobbyMessage message,
                                  @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getUserId(), null)) return;
        try {
            User user = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.rejoinLobby(message.getLobbyId(), user);
            playerSessionService.playerJoinedLobby(message.getUserId(), lobby.getId());
            LobbyResponse response = new LobbyResponse(true, "Rejoined lobby", lobby.getId(), lobby);
            sendToUser(message.getUserId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), null, null));
        }
    }

    @MessageMapping("/game/rejoin")
    public void handleRejoinGame(RejoinGameMessage message) {
        handleRejoinGame(message, null);
    }

    public void handleRejoinGame(RejoinGameMessage message,
                                 @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorized(sessionId, message.getUserId(), null)) return;
        try {
            String gameId = message.getGameId();

            if (gameId == null || gameId.isBlank()) {
                gameId = playerSessionService.getGameForPlayer(message.getUserId());
            }

            if (gameId == null) {
                String lobbyId = playerSessionService.getLobbyForPlayer(message.getUserId());
                if (lobbyId != null) {
                    Lobby lobby = lobbyService.getLobby(lobbyId);
                    if (lobby != null) {
                        sendToUser(message.getUserId(),
                                new LobbyResponse(true, "Rejoined lobby", lobby.getId(), lobby));
                        return;
                    }
                }
                sendToUser(message.getUserId(),
                        new LobbyResponse(false, "No active game or lobby found", null, null));
                return;
            }

            GameState gameState = gameController.getGame(gameId);
            if (gameState == null) {
                sendToUser(message.getUserId(),
                        new LobbyResponse(false, "Game not found", null, null));
                return;
            }

            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameId + "/movements",
                    GameStateMapper.toDto(gameState)
            );
        } catch (Exception e) {
            sendToUser(message.getUserId(),
                    new LobbyResponse(false, e.getMessage(), null, null));
        }
    }

    // ── Game endpoints ────────────────────────────────────────────────────────

    @MessageMapping("/game/{gameId}/state")
    public void handleGetGameState(@DestinationVariable String gameId) {
        GameState gameState = gameController.getGame(gameId);
        if (gameState != null) {
            broadcastGameState(gameId, gameState);
        }
    }

    @MessageMapping("/game/start-position/request")
    public void handleStartPositionRequest(StartPositionRequest request) {
        handleStartPositionRequest(request, null);
    }

    public void handleStartPositionRequest(StartPositionRequest request,
                                           @Header(value = "simpSessionId", required = false) String sessionId) {
        if (request == null) return;
        String gameId = request.getGameId();
        String playerId = request.getPlayerId();

        if (!validateStartPositionIds(gameId, playerId)) return;
        if (denyIfUnauthorizedGame(sessionId, playerId)) return;

        String topic = TOPIC_GAME + gameId + "/player/" + playerId + "/start-position";
        GameState gameState = resolveGameState(gameId, playerId, topic);
        if (gameState == null) return;

        try {
            int position = gameState.assignStartPosition(playerId, request.getSelectedStartPosition());
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("START_POSITION_ASSIGNED", gameId, playerId, position, null));
            broadcastGameState(gameId, gameState);
        } catch (Exception e) {
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse(ERROR_TYPE, gameId, playerId, null, e.getMessage()));
        }
    }

    @MessageMapping("/game/start-position/confirm")
    public void handleConfirmStartPosition(StartPositionConfirmRequest request) {
        handleConfirmStartPosition(request, null);
    }

    public void handleConfirmStartPosition(StartPositionConfirmRequest request,
                                           @Header(value = "simpSessionId", required = false) String sessionId) {
        if (request == null) return;
        String gameId = request.getGameId();
        String playerId = request.getPlayerId();

        if (!validateStartPositionIds(gameId, playerId)) return;
        if (denyIfUnauthorizedGame(sessionId, playerId)) return;

        String playerTopic = TOPIC_GAME + gameId + "/player/" + playerId + "/start-position";
        GameState gameState = resolveGameState(gameId, playerId, playerTopic);
        if (gameState == null) return;

        try {
            int confirmedPosition = gameState.confirmStartPosition(playerId, request.getStartPosition());
            messagingTemplate.convertAndSend(playerTopic,
                    new StartPositionResponse("START_POSITION_CONFIRMED", gameId, playerId, confirmedPosition, null));
        } catch (Exception e) {
            messagingTemplate.convertAndSend(playerTopic,
                    new StartPositionResponse(ERROR_TYPE, gameId, playerId, null, e.getMessage()));
        }
    }

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, @Payload MovementMessage movement) {
        handleMove(gameId, movement, null);
    }

    public void handleMove(@DestinationVariable String gameId, @Payload MovementMessage movement,
                           @Header(value = "simpSessionId", required = false) String sessionId) {
        if (movement == null) {
            sendMoveResponse(gameId, new MovementResponse(false, "NULL MESSAGE", 0, null));
            return;
        }
        if (movement.getPlayerId() == null) {
            sendMoveResponse(gameId, new MovementResponse(false, "Invalid movement data: No player ID", 0, null));
            return;
        }
        if (denyIfUnauthorizedGame(sessionId, movement.getPlayerId())) return;

        GameState gameState = gameController.getGame(gameId);

        try {
            if (gameState == null) {
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Game not found", 0, null));
                return;
            }

            Player movingPlayer = gameState.getPlayer(movement.getPlayerId());
            if (movingPlayer == null) {
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid movement data", 0, null));
                return;
            }

            boolean isMrX = movingPlayer.isMrX();
            TurnType phase = gameState.getCurrentPhase();

            if (isMrX && phase != TurnType.MRX) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Not Mr. X's turn", pos != null ? pos : 0, null));
                return;
            }

            if (!isMrX && phase != TurnType.DETECTIVES) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Not the detectives' turn", pos != null ? pos : 0, null));
                return;
            }

            if (!isMrX && !gameState.getRoundController().isDetectivePending(movement.getPlayerId())) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Detective has already moved this round", pos != null ? pos : 0, null));
                return;
            }

            if (movement.getTicket() == TicketType.DOUBLE) {
                Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
                int pos = playerPosition != null ? playerPosition : 0;

                if (!isMrX) {
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Only Mr. X can use the DOUBLE ticket", pos, null));
                    return;
                }
                if (!movingPlayer.hasTicket(TicketType.DOUBLE)) {
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "No DOUBLE tickets remaining", pos, null));
                    return;
                }
                if (gameState.getRoundController().isDoubleMoveActive()) {
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Double move is already in use", pos, null));
                    return;
                }

                boolean success = gameState.activateDoubleMove();
                if (success) {
                    broadcastGameState(gameId, gameState);
                    sendMoveResponse(gameId, new MovementResponse(true, "Double move ticket activated", pos, null));
                } else {
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Cannot activate double move ticket", pos, null));
                }
                return;
            }

            Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
            if (playerPosition == null) {
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid movement data", 0, null));
                return;
            }

            boolean success = gameState.movePlayer(movement.getPlayerId(), movement.getTicket(), movement.getTargetPosition());
            broadcastGameState(gameId, gameState);

            if (!success) {
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid move", gameState.getPlayerPosition(movement.getPlayerId()), null));
                return;
            }

            log.debug("Move executed successfully, new position={}", gameState.getPlayerPosition(movement.getPlayerId()));

            gameState.lockStuckDetectives();

            switch (gameState.checkGameResult()) {
                case DETECTIVES_WIN -> {
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "DETECTIVES_WIN");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Detectives win!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    gameController.removeGame(gameId);
                    return;
                }
                case MRX_WINS -> {
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "MRX_WINS");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Mr. X wins!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    gameController.removeGame(gameId);
                    return;
                }
            }

            String extra = (isMrX && gameState.getRoundController().isDoubleMoveActive())
                    ? " (1 move remaining due to double move ticket)" : "";
            sendMoveResponse(gameId, new MovementResponse(true, "Movement successful" + extra, gameState.getPlayerPosition(movement.getPlayerId()), null));

        } catch (Exception e) {
            sendToUser(movement.getPlayerId(), new MovementResponse(false, "Error: " + e.getMessage(), 0, null));
        }
    }

    @MessageMapping("/game/kickPlayer")
    public void handleKickPlayerInGame(KickPlayerInGameMessage message) {
        handleKickPlayerInGame(message, null);
    }

    public void handleKickPlayerInGame(KickPlayerInGameMessage message,
                                       @Header(value = "simpSessionId", required = false) String sessionId) {
        if (denyIfUnauthorizedGame(sessionId, message.getRequesterId())) return;
        try {
            GameState gameState = gameController.getGame(message.getGameId());
            if (gameState == null) {
                sendToUser(message.getRequesterId(), new MovementResponse(false, "Game not found", 0, null));
                return;
            }
            String result = gameState.kickPlayer(message.getRequesterId(), message.getTargetId());
            broadcastGameState(message.getGameId(), gameState);

            switch (result) {
                case "MRX_KICKED" -> {
                    broadcastGameOver(message.getGameId(), "DETECTIVES_WIN");
                    gameController.removeGame(message.getGameId());
                }
                case "TOO_FEW_PLAYERS" -> {
                    broadcastGameOver(message.getGameId(), "DETECTIVES_WIN");
                    gameController.removeGame(message.getGameId());
                }
                default -> { /* continue game */ }
            }
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new MovementResponse(false, e.getMessage(), 0, null));
        }
    }

    @MessageMapping("/game/delete") // TODO: Testing
    public void handleDeleteGame(DeleteGameMessage message) {
        handleDeleteGame(message, null);
    }

    public void handleDeleteGame(DeleteGameMessage message, @Header(value = "simpSessionId", required = false) String sessionId) {  // TODO: TESTING
        if (denyIfUnauthorizedGame(sessionId, message.getRequesterId())) return;
        try {
            GameState gameState = gameController.getGame(message.getGameId());
            if (gameState == null) {
                sendToUser(message.getRequesterId(), new MovementResponse(false, "Game not found", 0, null));
                return;
            }
            if (!message.getRequesterId().equals(gameState.getHostId())) {
                sendToUser(message.getRequesterId(), new MovementResponse(false, "Only the host can delete the game", 0, null));
                return;
            }

            gameController.removeGame(message.getGameId());
            try {
                // Lobby-ID entspricht der Game-ID (siehe handleStartGame); Lobby existiert
                // ggf. noch parallel zum Spiel und muss separat entfernt werden.
                lobbyService.deleteLobby(message.getGameId(), message.getRequesterId());
            } catch (Exception ignored) {
                // Lobby war ggf. bereits entfernt - unkritisch, das Spiel ist bereits gelöscht.
            }

            broadcastGameOver(message.getGameId(), "GAME_DELETED");
        } catch (Exception e) {
            sendToUser(message.getRequesterId(), new MovementResponse(false, e.getMessage(), 0, null));
        }
    }
}