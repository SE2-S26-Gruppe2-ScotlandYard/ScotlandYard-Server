package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StartPositionRequest;
import at.aau.serg.websocketdemoserver.dtos.StartPositionResponse;
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
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import at.aau.serg.websocketdemoserver.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketBrokerController {

    private static final String TOPIC_GAME = "/topic/game/";

    private final GameController gameController = GameController.getInstance();
    private final LobbyService lobbyService = new LobbyService();
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService = new UserService();

    public WebSocketBrokerController() {
        this.messagingTemplate = null;
    }

    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/user/connect")
    @SendTo("/topic/user-response")
    public UserConnectResponse handleUserConnect(UserConnectMessage message) {
        try {
            User user = userService.registerUser(message.getNickName());
            return new UserConnectResponse(true, "User registered", user);
        } catch (IllegalArgumentException e) {
            // Wenn der Name vergeben oder leer ist
            return new UserConnectResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            return new UserConnectResponse(false, "Internal Server Error", null);
        }
    }

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

    @MessageMapping("/move")
    @SendTo("/topic/move-response")
    public MovementResponse handleMove(@Payload MovementMessage movement) {

        if (movement == null) {
            return new MovementResponse(false, "NULL MESSAGE", 0, null);
        }

        if (movement.getGameId() == null || movement.getPlayerId() == null) {
            return new MovementResponse(false, "Invalid movement data", 0, null);
        }

        GameState gameState = gameController.getGame(movement.getGameId());

        try {
            if (gameState == null) {
                return new MovementResponse(false, "Game not found", 0, null);
            }

            Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
            if (playerPosition == null) {
                return new MovementResponse(false, "Invalid movement data", 0, null);
            }

            boolean isMrX = gameState.getPlayer(movement.getPlayerId()) != null
                    && gameState.getPlayer(movement.getPlayerId()).isMrX();

            TurnType phase = gameState.getCurrentPhase();

            if (isMrX && phase != TurnType.MRX) {
                return new MovementResponse(
                        false,
                        "Not Mr. X's turn",
                        playerPosition,
                        null
                );
            }

            if (!isMrX && phase != TurnType.DETECTIVES) {
                return new MovementResponse(
                        false,
                        "Not the detectives' turn",
                        playerPosition,
                        null
                );
            }

            if (!isMrX && !gameState.getRoundController().isDetectivePending(movement.getPlayerId())) {
                return new MovementResponse(
                        false,
                        "Detective has already moved this round",
                        playerPosition,
                        null
                );
            }

            if (movement.getTicket() == TicketType.DOUBLE) {
                boolean success = gameState.activateDoubleMove();

                if (!success) {
                    Player player = gameState.getPlayer(movement.getPlayerId());
                    if (!player.isMrX()) {
                        return new MovementResponse(false, "Only Mr. X can use the DOUBLE ticket", playerPosition, null);
                    }
                    if (!player.hasTicket(TicketType.DOUBLE)) {
                        return new MovementResponse(false, "No DOUBLE tickets remaining", playerPosition, null);
                    }
                    if (gameState.getRoundController().isDoubleMoveActive()) {
                        return new MovementResponse(false, "Double move is already in use", playerPosition, null);
                    }
                    return new MovementResponse(false, "Cannot activate double move ticket", playerPosition, null);
                }

                broadcastGameState(movement.getGameId(), gameState);

                return new MovementResponse(true, "Double move ticket activated", playerPosition, null);
            }

            boolean success = gameState.movePlayer(
                    movement.getPlayerId(),
                    movement.getTicket(),
                    movement.getTargetPosition()
            );

            broadcastGameState(movement.getGameId(), gameState);

            if (!success) {
                return new MovementResponse(
                        false,
                        "Invalid move",
                        gameState.getPlayerPosition(movement.getPlayerId()),
                        null
                );
            }

            switch (gameState.checkGameResult()) {
                case DETECTIVES_WIN -> {
                    broadcastGameState(movement.getGameId(), gameState);
                    broadcastGameOver(movement.getGameId(), "DETECTIVES_WIN");
                    return new MovementResponse(true, "Movement successful: Detectives win!", gameState.getPlayerPosition(movement.getPlayerId()), null);
                }
                case MRX_WINS -> {
                    broadcastGameState(movement.getGameId(), gameState);
                    broadcastGameOver(movement.getGameId(), "MRX_WINS");
                    return new MovementResponse(true, "Movement successful: Mr. X wins!", gameState.getPlayerPosition(movement.getPlayerId()), null);
                }
            }

            String extra = (isMrX && gameState.getRoundController().isDoubleMoveActive())
                    ? " (1 move remaining due to double move ticket)" : "";

            return new MovementResponse(
                    true,
                    "Movement successful" + extra,
                    gameState.getPlayerPosition(movement.getPlayerId()),
                    null
            );

        } catch (Exception e) {
            return new MovementResponse(false, "Error: " + e.getMessage(), 0, null);
        }
    }

    @MessageMapping("/lobby/create")
    @SendTo("/topic/lobby")
    public LobbyResponse handleCreateLobby(CreateLobbyMessage message) {
        try {
            User host = new User(
                    message.getUserId(),
                    message.getNickName()
            );

            Lobby lobby = lobbyService.createLobby(message.getLobbyName(), host);

            return new LobbyResponse(true, "Lobby created", lobby.getId(), lobby);
        } catch (Exception e) {
            return new LobbyResponse(false, e.getMessage(), null, null);
        }
    }

    @MessageMapping("/lobby/join")
    @SendTo("/topic/lobby")
    public LobbyResponse handleJoinLobby(JoinLobbyMessage message) {
        try {
            User user = new User(
                    message.getUserId(),
                    message.getNickName()
            );

            Lobby lobby = lobbyService.joinLobby(message.getLobbyId(), user);

            return new LobbyResponse(true, "Joined lobby", lobby.getId(), lobby);
        } catch (Exception e) {
            return new LobbyResponse(false, e.getMessage(), null, null);
        }
    }

    @MessageMapping("/lobby/leave")
    @SendTo("/topic/lobby")
    public LobbyResponse handleLeaveLobby(LeaveLobbyMessage message) {
        try {
            lobbyService.leaveLobby(message.getLobbyId(), message.getUserId());

            Lobby updatedLobby = lobbyService.getLobby(message.getLobbyId());

            if (updatedLobby == null) {
                return new LobbyResponse(true, "Lobby deleted (empty)", message.getLobbyId(), null);
            }

            return new LobbyResponse(true, "Left lobby", updatedLobby.getId(), updatedLobby);
        } catch (Exception e) {
            return new LobbyResponse(false, e.getMessage(), null, null);
        }
    }

    @MessageMapping("/lobby/delete")
    @SendTo("/topic/lobby")
    public LobbyResponse handleDeleteLobby(DeleteLobbyMessage message) {
        try {
            lobbyService.deleteLobby(message.getLobbyId(), message.getRequesterId());

            return new LobbyResponse(true, "Lobby deleted", message.getLobbyId(), null);
        } catch (Exception e) {
            return new LobbyResponse(false, e.getMessage(), null, null);
        }
    }

    @MessageMapping("/game/start-position/request")
    public void handleStartPositionRequest(@Payload StartPositionRequest request) {
        String destination = TOPIC_GAME + request.getGameId()
                + "/player/" + request.getPlayerId() + "/start-position";

        GameState gameState = gameController.getGame(request.getGameId());

        if (gameState == null) {
            sendStartPositionResponse(destination,
                    new StartPositionResponse("ERROR", request.getGameId(), request.getPlayerId(),
                            null, "Game not found"));
            return;
        }

        try {
            int position = gameState.assignStartPosition(request.getPlayerId());
            sendStartPositionResponse(destination,
                    new StartPositionResponse("START_POSITION_ASSIGNED", request.getGameId(),
                            request.getPlayerId(), position, "Start position assigned"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendStartPositionResponse(destination,
                    new StartPositionResponse("ERROR", request.getGameId(), request.getPlayerId(),
                            null, e.getMessage()));
        }
    }

    private void sendStartPositionResponse(String destination, StartPositionResponse response) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend(destination, response);
        }
    }

    private void broadcastGameState(String gameId, GameState gameState) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend(TOPIC_GAME + gameId, gameState);
        }
    }

    private void broadcastGameOver(String gameId, String result) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend(TOPIC_GAME + gameId + "/over", result);
        }
    }
}