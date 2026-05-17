package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.game.GameStateDto;
import at.aau.serg.websocketdemoserver.dtos.game.StartPositionRequest;
import at.aau.serg.websocketdemoserver.dtos.game.StartPositionResponse;
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
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import at.aau.serg.websocketdemoserver.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketBrokerController {

    private static final String TOPIC_GAME = "/topic/game/";

    private final GameController gameController;
    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate) {
        this(messagingTemplate, new GameController(), new LobbyService(), new UserService());
    }

    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate,
                                     GameController gameController,
                                     LobbyService lobbyService,
                                     UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.gameController = gameController;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    // 1. Private Antworten an den Auslöser über das dedizierte Player-Topic
    private void sendToUser(String userId, Object payload) {
        System.out.println("[DEBUG] Sending to user [" + userId + "]: " + payload);
        messagingTemplate.convertAndSend("/topic/player/" + userId, payload);
    }

    // 2. Globale Antworten (NUR für den Server-Browser: Erstellen & Löschen)
    private void broadcastToGlobalLobbyList(LobbyResponse response) {
        System.out.println("[DEBUG] Broadcasting to global lobby list: " + response);
        messagingTemplate.convertAndSend("/topic/lobby", response);
    }

    // 3. Isolierte Antworten (NUR für die Spieler, die physisch IN dieser Lobby sind)
    private void broadcastToSpecificLobby(String lobbyId, LobbyResponse response) {
        System.out.println("[DEBUG] Broadcasting to lobby [" + lobbyId + "]: " + response);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, response);
    }

    private void sendMoveResponse(String gameId, MovementResponse response) {
        System.out.println("[DEBUG] Sending move response for game [" + gameId + "]: " + response);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/move-response", response);
    }

    private void broadcastGameState(String gameId, GameState gameState) {
        // ✅ GameStateDto statt GameState – korrekte JSON-Struktur für den Client
        GameStateDto dto = GameStateMapper.toDto(gameState);
        System.out.println("[DEBUG] Broadcasting GameStateDto for game [" + gameId + "]\n" +
                        "GameState: [Round: " + dto.getCurrentRound() + "], [Phase: " + dto.getCurrentPhase() + "], [DetectivePositions: " + dto.getDetectivePositions() + "], ");
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/movements", dto);
    }

    private void broadcastGameOver(String gameId, String result) {
        System.out.println("[DEBUG] GAME OVER for game [" + gameId + "]: Result=" + result);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/over", result);
    }

    @MessageMapping("/hello")
    @SendTo("/topic/hello-response")
    public String handleHello(String text) {
        System.out.println("[DEBUG] Received /hello request: text='" + text + "'");
        String response = "echo from broker: " + text;
        System.out.println("[DEBUG] Sending /hello response: " + response);
        return response;
    }

    @MessageMapping("/object")
    @SendTo("/topic/rcv-object")
    public StompMessage handleObject(StompMessage msg) {
        System.out.println("[DEBUG] Received /object request: " + msg);
        System.out.println("[DEBUG] Sending /object response: " + msg);
        return msg;
    }

    @MessageMapping("/user/connect")
    @SendToUser("/topic/user-response")
    public UserConnectResponse handleUserConnect(UserConnectMessage message) {
        System.out.println("[DEBUG] Received /user/connect request: nickName='" + message.getNickName() + "'");
        try {
            User user = userService.registerUser(message.getNickName());
            UserConnectResponse response = new UserConnectResponse(true, "User registered", user);
            System.out.println("[DEBUG] Sending user connect response (success): userId=" + user.id() + ", nickName=" + user.nickName());
            return response;
        } catch (IllegalArgumentException e) {
            UserConnectResponse response = new UserConnectResponse(false, e.getMessage(), null);
            System.out.println("[DEBUG] Sending user connect response (error): " + e.getMessage());
            return response;
        } catch (Exception e) {
            UserConnectResponse response = new UserConnectResponse(false, "Internal Server Error", null);
            System.out.println("[DEBUG] Sending user connect response (internal error)");
            return response;
        }
    }

    @MessageMapping("/lobby/create")
    public void handleCreateLobby(CreateLobbyMessage message) {
        System.out.println("[DEBUG] Received /lobby/create request: lobbyName='" + message.getLobbyName() +
                "', userId='" + message.getUserId() + "', nickName='" + message.getNickName() + "'");
        try {
            User host = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.createLobby(message.getLobbyName(), host);
            LobbyResponse response = new LobbyResponse(true, message.getNickName() + "'s Lobby created", lobby.getId(), lobby);
            System.out.println("[DEBUG] Lobby created successfully: lobbyId=" + lobby.getId() + ", lobbyName=" + message.getLobbyName());

            sendToUser(message.getUserId(), response);
            broadcastToGlobalLobbyList(response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error creating lobby: " + e.getMessage());
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), null, null));
        }
    }

    @MessageMapping("/lobby/join")
    public void handleJoinLobby(JoinLobbyMessage message) {
        System.out.println("[DEBUG] Received /lobby/join request: lobbyId='" + message.getLobbyId() +
                "', userId='" + message.getUserId() + "', nickName='" + message.getNickName() + "'");
        try {
            User user = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.joinLobby(message.getLobbyId(), user);
            String hostName = lobby.getHost().nickName();
            String responseMessage = message.getNickName() + " joined " + hostName + "'s Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            System.out.println("[DEBUG] User joined lobby: lobbyId=" + lobby.getId() + ", currentUsers=" + lobby.getUsers().size());

            sendToUser(message.getUserId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error joining lobby: " + e.getMessage());
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/leave")
    public void handleLeaveLobby(LeaveLobbyMessage message) {
        System.out.println("[DEBUG] Received /lobby/leave request: lobbyId='" + message.getLobbyId() +
                "', userId='" + message.getUserId() + "'");
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            User leavingUser = (lobbyBefore != null) ? lobbyBefore.getUsers().stream()
                    .filter(u -> u.id().equals(message.getUserId()))
                    .findFirst().orElse(null) : null;
            String leavingUserName = (leavingUser != null) ? leavingUser.nickName() : "Unknown";
            String hostNameBefore = (lobbyBefore != null && lobbyBefore.getHost() != null) ? lobbyBefore.getHost().nickName() : "Unknown";
            System.out.println("[DEBUG] User leaving: " + leavingUserName + " from lobby hosted by " + hostNameBefore);

            lobbyService.leaveLobby(message.getLobbyId(), message.getUserId());
            Lobby updatedLobby = lobbyService.getLobby(message.getLobbyId());

            if (updatedLobby == null) {
                System.out.println("[DEBUG] Lobby is now empty and will be cleaned up");
                String responseMessage = leavingUserName + " left " + hostNameBefore + "'s Lobby (Lobby is now empty)";
                LobbyResponse response = new LobbyResponse(true, responseMessage, message.getLobbyId(), null);
                sendToUser(message.getUserId(), response);
                broadcastToGlobalLobbyList(response);
            } else {
                String hostNameAfter = updatedLobby.getHost().nickName();
                String responseMessage = leavingUserName + " left " + hostNameAfter + "'s Lobby";
                LobbyResponse response = new LobbyResponse(true, responseMessage, updatedLobby.getId(), updatedLobby);
                System.out.println("[DEBUG] User left, lobby still active with " + updatedLobby.getUsers().size() + " users");
                sendToUser(message.getUserId(), response);
                broadcastToSpecificLobby(updatedLobby.getId(), response);
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Error leaving lobby: " + e.getMessage());
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/delete")
    public void handleDeleteLobby(DeleteLobbyMessage message) {
        System.out.println("[DEBUG] Received /lobby/delete request: lobbyId='" + message.getLobbyId() +
                "', requesterId='" + message.getRequesterId() + "'");
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            String hostName = (lobbyBefore != null && lobbyBefore.getHost() != null) ? lobbyBefore.getHost().nickName() : "Unknown";
            System.out.println("[DEBUG] Deleting lobby hosted by: " + hostName);

            lobbyService.deleteLobby(message.getLobbyId(), message.getRequesterId());
            String responseMessage = hostName + " deleted the Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, message.getLobbyId(), null);
            System.out.println("[DEBUG] Lobby deleted successfully");

            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(message.getLobbyId(), response);
            broadcastToGlobalLobbyList(response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error deleting lobby: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/kick")
    public void handleKickPlayer(KickPlayerMessage message) {
        System.out.println("[DEBUG] Received /lobby/kick request: lobbyId='" + message.getLobbyId() +
                "', targetUserId='" + message.getTargetUserId() + "', requesterId='" + message.getRequesterId() + "'");
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            User targetUser = lobbyBefore.getUsers().stream()
                    .filter(u -> u.id().equals(message.getTargetUserId()))
                    .findFirst().orElse(null);
            String targetNickName = (targetUser != null) ? targetUser.nickName() : "Unknown";
            System.out.println("[DEBUG] Kicking player: " + targetNickName);

            Lobby lobby = lobbyService.kickPlayer(
                    message.getLobbyId(),
                    message.getRequesterId(),
                    message.getTargetUserId()
            );
            String hostName = lobby.getHost().nickName();
            String responseMessage = targetNickName + " was kicked out of " + hostName + "'s Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            System.out.println("[DEBUG] Player kicked successfully");

            sendToUser(message.getTargetUserId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error kicking player: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/setRole")
    public void handleSetRole(SetRoleMessage message) {
        System.out.println("[DEBUG] Received /lobby/setRole request: lobbyId='" + message.getLobbyId() +
                "', targetUserId='" + message.getTargetUserId() + "', role='" + message.getRole() +
                "', requesterId='" + message.getRequesterId() + "'");
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
            System.out.println("[DEBUG] Role set successfully: " + targetNickName + " -> " + message.getRole());
            broadcastToSpecificLobby(lobby.getId(), new LobbyResponse(true, responseMessage, lobby.getId(), lobby));
        } catch (Exception e) {
            System.out.println("[DEBUG] Error setting role: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/startRoleSelection")
    public void handleStartRoleSelection(StartRoleSelectionMessage message) {
        System.out.println("[DEBUG] Received /lobby/startRoleSelection request: lobbyId='" + message.getLobbyId() +
                "', requesterId='" + message.getRequesterId() + "'");
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can start role selection");
            System.out.println("[DEBUG] Starting role selection for lobby: " + lobby.getId());

            lobby.setLocked(true);
            String hostName = lobby.getHost().nickName();
            String responseMessage = hostName + " started role selection";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            System.out.println("[DEBUG] Role selection started");

            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error starting role selection: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/lobby/startGame")
    public void handleStartGame(StartGameMessage message) {
        if (message == null) {
            System.out.println("[DEBUG] Received null StartGameMessage");
            return;
        }
        System.out.println("[DEBUG] Received /lobby/startGame request: lobbyId='" + message.getLobbyId() +
                "', requesterId='" + message.getRequesterId() + "'");
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can start the game");

            System.out.println("[DEBUG] Starting game for lobby: " + lobby.getId() + " with " + lobby.getUsers().size() + " players");

            GameState gameState = new GameState(lobby.getId());
            gameState.initializePlayersFromLobby(lobby);
            gameController.addGame(lobby.getId(), gameState);
            System.out.println("[DEBUG] Game initialized with ID: " + lobby.getId());

            broadcastToSpecificLobby(lobby.getId(), new LobbyResponse(true, "GAME_STARTED", lobby.getId(), lobby));

            broadcastGameState(lobby.getId(), gameState);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error starting game: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/game/{gameId}/state")
    public void handleGetGameState(@DestinationVariable String gameId) {
        GameState gameState = gameController.getGame(gameId);
        if (gameState != null) {
            broadcastGameState(gameId, gameState);
        }
    }

    @MessageMapping("/lobby/backToLobby")
    public void handleBackToLobby(BackToLobbyMessage message) {
        System.out.println("[DEBUG] Received /lobby/backToLobby request: lobbyId='" + message.getLobbyId() +
                "', requesterId='" + message.getRequesterId() + "'");
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can go back to lobby");
            System.out.println("[DEBUG] Returning to lobby: " + lobby.getId());

            lobby.setLocked(false);
            String hostName = lobby.getHost().nickName();
            String responseMessage = hostName + " returned to Lobby";
            LobbyResponse response = new LobbyResponse(true, responseMessage, lobby.getId(), lobby);
            System.out.println("[DEBUG] Returned to lobby mode");

            sendToUser(message.getRequesterId(), response);
            broadcastToSpecificLobby(lobby.getId(), response);
        } catch (Exception e) {
            System.out.println("[DEBUG] Error returning to lobby: " + e.getMessage());
            sendToUser(message.getRequesterId(), new LobbyResponse(false, e.getMessage(), message.getLobbyId(), null));
        }
    }

    @MessageMapping("/game/start-position/request")
    public void handleStartPositionRequest(StartPositionRequest request) {
        System.out.println("[DEBUG] Received /game/start-position/request: gameId='" + request.getGameId() +
                "', playerId='" + request.getPlayerId() + "'");
        String gameId = request.getGameId();
        String playerId = request.getPlayerId();
        String topic = TOPIC_GAME + gameId + "/player/" + playerId + "/start-position";

        GameState gameState = gameController.getGame(gameId);
        if (gameState == null) {
            System.out.println("[DEBUG] Game not found for ID: " + gameId);
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("ERROR", gameId, playerId, null, "Game not found"));
            return;
        }

        try {
            int position = gameState.assignStartPosition(playerId);
            System.out.println("[DEBUG] Start position assigned: player=" + playerId + ", position=" + position);
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("START_POSITION_ASSIGNED", gameId, playerId, position, null));
        } catch (Exception e) {
            System.out.println("[DEBUG] Error assigning start position: " + e.getMessage());
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("ERROR", gameId, playerId, null, e.getMessage()));
        }
    }

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, @Payload MovementMessage movement) {
        System.out.println("[DEBUG] Received /game/" + gameId + "/move request: playerId='" +
                (movement != null ? movement.getPlayerId() : "null") +
                "', ticket=" + (movement != null ? movement.getTicket() : "null") +
                ", targetPos=" + (movement != null ? movement.getTargetPosition() : "null"));

        if (movement == null) {
            System.out.println("[DEBUG] Received null MovementMessage");
            sendMoveResponse(gameId, new MovementResponse(false, "NULL MESSAGE", 0, null));
            return;
        }
        if (movement.getPlayerId() == null) {
            System.out.println("[DEBUG] Movement message missing playerId");
            sendMoveResponse(gameId, new MovementResponse(false, "Invalid movement data: No player ID", 0, null));
            return;
        }

        GameState gameState = gameController.getGame(gameId);

        try {
            if (gameState == null) {
                System.out.println("[DEBUG] Game not found for ID: " + gameId);
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Game not found", 0, null));
                return;
            }

            Player movingPlayer = gameState.getPlayer(movement.getPlayerId());
            if (movingPlayer == null) {
                System.out.println("[DEBUG] Player not found: " + movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid movement data", 0, null));
                return;
            }

            boolean isMrX = movingPlayer.isMrX();
            TurnType phase = gameState.getCurrentPhase();
            System.out.println("[DEBUG] Move attempt: player=" + movement.getPlayerId() +
                    ", isMrX=" + isMrX + ", currentPhase=" + phase +
                    ", round=" + gameState.getRoundController().getCurrentRound());

            if (isMrX && phase != TurnType.MRX) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                System.out.println("[DEBUG] Not Mr. X's turn (phase=" + phase + ")");
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Not Mr. X's turn", pos != null ? pos : 0, null));
                return;
            }

            if (!isMrX && phase != TurnType.DETECTIVES) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                System.out.println("[DEBUG] Not the detectives' turn (phase=" + phase + ")");
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Not the detectives' turn", pos != null ? pos : 0, null));
                return;
            }

            if (!isMrX && !gameState.getRoundController().isDetectivePending(movement.getPlayerId())) {
                Integer pos = gameState.getPlayerPosition(movement.getPlayerId());
                System.out.println("[DEBUG] Detective already moved this round: " + movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Detective has already moved this round", pos != null ? pos : 0, null));
                return;
            }

            if (movement.getTicket() == TicketType.DOUBLE) {
                Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
                int pos = playerPosition != null ? playerPosition : 0;
                System.out.println("[DEBUG] DOUBLE ticket requested by player: " + movement.getPlayerId());

                if (!isMrX) {
                    System.out.println("[DEBUG] Only Mr. X can use DOUBLE ticket");
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Only Mr. X can use the DOUBLE ticket", pos, null));
                    return;
                }
                if (!movingPlayer.hasTicket(TicketType.DOUBLE)) {
                    System.out.println("[DEBUG] No DOUBLE tickets remaining for Mr. X");
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "No DOUBLE tickets remaining", pos, null));
                    return;
                }
                if (gameState.getRoundController().isDoubleMoveActive()) {
                    System.out.println("[DEBUG] Double move already active");
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Double move is already in use", pos, null));
                    return;
                }

                boolean success = gameState.activateDoubleMove();
                if (success) {
                    System.out.println("[DEBUG] DOUBLE ticket activated successfully");
                    broadcastGameState(gameId, gameState);
                    sendMoveResponse(gameId, new MovementResponse(true, "Double move ticket activated", pos, null));
                } else {
                    System.out.println("[DEBUG] Failed to activate DOUBLE ticket");
                    sendToUser(movement.getPlayerId(), new MovementResponse(false, "Cannot activate double move ticket", pos, null));
                }
                return;
            }

            Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
            if (playerPosition == null) {
                System.out.println("[DEBUG] Invalid player position for: " + movement.getPlayerId());
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid movement data", 0, null));
                return;
            }

            System.out.println("[DEBUG] Executing move: player=" + movement.getPlayerId() +
                    ", from=" + playerPosition + ", to=" + movement.getTargetPosition() +
                    ", ticket=" + movement.getTicket());

            boolean success = gameState.movePlayer(movement.getPlayerId(), movement.getTicket(), movement.getTargetPosition());
            broadcastGameState(gameId, gameState);

            if (!success) {
                System.out.println("[DEBUG] Move execution failed");
                sendToUser(movement.getPlayerId(), new MovementResponse(false, "Invalid move", gameState.getPlayerPosition(movement.getPlayerId()), null));
                return;
            }

            System.out.println("[DEBUG] Move executed successfully, new position=" + gameState.getPlayerPosition(movement.getPlayerId()));

            switch (gameState.checkGameResult()) {
                case DETECTIVES_WIN -> {
                    System.out.println("[DEBUG] GAME OVER - Detectives win!");
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "DETECTIVES_WIN");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Detectives win!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    return;
                }
                case MRX_WINS -> {
                    System.out.println("[DEBUG] GAME OVER - Mr. X wins!");
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "MRX_WINS");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Mr. X wins!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    return;
                }
            }

            String extra = (isMrX && gameState.getRoundController().isDoubleMoveActive())
                    ? " (1 move remaining due to double move ticket)" : "";

            if (gameState.getRoundController().isDoubleMoveActive()) {
                System.out.println("[DEBUG] Double move active, waiting for second move");
            }

            sendMoveResponse(gameId, new MovementResponse(true, "Movement successful" + extra, gameState.getPlayerPosition(movement.getPlayerId()), null));

        } catch (Exception e) {
            System.out.println("[DEBUG] Exception during move handling: " + e.getMessage());
            e.printStackTrace();
            sendToUser(movement.getPlayerId(), new MovementResponse(false, "Error: " + e.getMessage(), 0, null));
        }
    }
}