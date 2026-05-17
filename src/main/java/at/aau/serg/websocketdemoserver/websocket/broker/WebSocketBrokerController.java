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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate) {
        this(messagingTemplate, GameController.getInstance(), new LobbyService(), new UserService());
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
        messagingTemplate.convertAndSend("/topic/player/" + userId, payload);
    }

    // 2. Globale Antworten (NUR für den Server-Browser: Erstellen & Löschen)
    private void broadcastToGlobalLobbyList(LobbyResponse response) {
        messagingTemplate.convertAndSend("/topic/lobby", response);
    }

    // 3. Isolierte Antworten (NUR für die Spieler, die physisch IN dieser Lobby sind)
    private void broadcastToSpecificLobby(String lobbyId, LobbyResponse response) {
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, response);
    }

    private void sendMoveResponse(String gameId, MovementResponse response) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/move-response", response);
    }

    private void broadcastGameState(String gameId, GameState gameState) {
        GameStateDto dto = GameStateMapper.toDto(gameState);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/movements", dto);
    }

    private void broadcastGameOver(String gameId, String result) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/over", result);
    }

    @MessageMapping("/hello")
    @SendTo("/topic/hello-response")
    public String handleHello(String text) {
        String response = "echo from broker: " + text;
        return response;
    }

    @MessageMapping("/object")
    @SendTo("/topic/rcv-object")
    public StompMessage handleObject(StompMessage msg) {
        return msg;
    }

    @MessageMapping("/user/connect")
    @SendToUser("/topic/user-response")
    public UserConnectResponse handleUserConnect(UserConnectMessage message) {
        try {
            User user = userService.registerUser(message.getNickName());
            UserConnectResponse response = new UserConnectResponse(true, "User registered", user);
            return response;
        } catch (IllegalArgumentException e) {
            UserConnectResponse response = new UserConnectResponse(false, e.getMessage(), null);
            return response;
        } catch (Exception e) {
            UserConnectResponse response = new UserConnectResponse(false, "Internal Server Error", null);
            return response;
        }
    }

    @MessageMapping("/lobby/create")
    public void handleCreateLobby(CreateLobbyMessage message) {
        try {
            User host = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.createLobby(message.getLobbyName(), host);
            LobbyResponse response = new LobbyResponse(true, message.getNickName() + "'s Lobby created", lobby.getId(), lobby);

            sendToUser(message.getUserId(), response);
            broadcastToGlobalLobbyList(response);
        } catch (Exception e) {
            sendToUser(message.getUserId(), new LobbyResponse(false, e.getMessage(), null, null));
        }
    }

    @MessageMapping("/lobby/join")
    public void handleJoinLobby(JoinLobbyMessage message) {
        try {
            User user = new User(message.getUserId(), message.getNickName());
            Lobby lobby = lobbyService.joinLobby(message.getLobbyId(), user);
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
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            User leavingUser = (lobbyBefore != null) ? lobbyBefore.getUsers().stream()
                    .filter(u -> u.id().equals(message.getUserId()))
                    .findFirst().orElse(null) : null;
            String leavingUserName = (leavingUser != null) ? leavingUser.nickName() : "Unknown";
            String hostNameBefore = (lobbyBefore != null && lobbyBefore.getHost() != null) ? lobbyBefore.getHost().nickName() : "Unknown";

            lobbyService.leaveLobby(message.getLobbyId(), message.getUserId());
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
        try {
            Lobby lobbyBefore = lobbyService.getLobby(message.getLobbyId());
            String hostName = (lobbyBefore != null && lobbyBefore.getHost() != null) ? lobbyBefore.getHost().nickName() : "Unknown";

            lobbyService.deleteLobby(message.getLobbyId(), message.getRequesterId());
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
        if (message == null) {
            return;
        }
        try {
            Lobby lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) throw new IllegalArgumentException("Lobby not found");
            if (!lobby.getHostId().equals(message.getRequesterId()))
                throw new IllegalStateException("Only host can start the game");


            GameState gameState = new GameState(lobby.getId());
            gameState.initializePlayersFromLobby(lobby);
            gameController.addGame(lobby.getId(), gameState);

            broadcastToSpecificLobby(lobby.getId(), new LobbyResponse(true, "GAME_STARTED", lobby.getId(), lobby));

            broadcastGameState(lobby.getId(), gameState);
        } catch (Exception e) {
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

    @MessageMapping("/game/start-position/request")
    public void handleStartPositionRequest(StartPositionRequest request) {
        String gameId = request.getGameId();
        String playerId = request.getPlayerId();
        String topic = TOPIC_GAME + gameId + "/player/" + playerId + "/start-position";

        GameState gameState = gameController.getGame(gameId);
        if (gameState == null) {
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("ERROR", gameId, playerId, null, "Game not found"));
            return;
        }

        try {
            int position = gameState.assignStartPosition(playerId);
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("START_POSITION_ASSIGNED", gameId, playerId, position, null));
        } catch (Exception e) {
            messagingTemplate.convertAndSend(topic,
                    new StartPositionResponse("ERROR", gameId, playerId, null, e.getMessage()));
        }
    }

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, @Payload MovementMessage movement) {

        if (movement == null) {
            sendMoveResponse(gameId, new MovementResponse(false, "NULL MESSAGE", 0, null));
            return;
        }
        if (movement.getPlayerId() == null) {
            sendMoveResponse(gameId, new MovementResponse(false, "Invalid movement data: No player ID", 0, null));
            return;
        }

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

            System.out.println("[DEBUG] Move executed successfully, new position=" + gameState.getPlayerPosition(movement.getPlayerId()));

            switch (gameState.checkGameResult()) {
                case DETECTIVES_WIN -> {
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "DETECTIVES_WIN");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Detectives win!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    return;
                }
                case MRX_WINS -> {
                    broadcastGameState(gameId, gameState);
                    broadcastGameOver(gameId, "MRX_WINS");
                    sendMoveResponse(gameId, new MovementResponse(true, "Movement successful: Mr. X wins!", gameState.getPlayerPosition(movement.getPlayerId()), null));
                    return;
                }
            }

            String extra = (isMrX && gameState.getRoundController().isDoubleMoveActive())
                    ? " (1 move remaining due to double move ticket)" : "";

            if (gameState.getRoundController().isDoubleMoveActive()) {
            }

            sendMoveResponse(gameId, new MovementResponse(true, "Movement successful" + extra, gameState.getPlayerPosition(movement.getPlayerId()), null));

        } catch (Exception e) {
            sendToUser(movement.getPlayerId(), new MovementResponse(false, "Error: " + e.getMessage(), 0, null));
        }
    }
}