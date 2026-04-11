package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.CreateLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.DeleteLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.JoinLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LeaveLobbyMessage;
import at.aau.serg.websocketdemoserver.dtos.lobby.LobbyResponse;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.service.LobbyService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketBrokerController {

    private final GameController gameController = GameController.getInstance();
    private final LobbyService lobbyService = new LobbyService();
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBrokerController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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
    public void handleMove(@Payload MovementMessage movement) {
        if (movement == null) {
            messagingTemplate.convertAndSend("/topic/errors", "NULL MESSAGE");
            return;
        }

        if (movement.getGameId() == null || movement.getPlayerId() == null) {
            messagingTemplate.convertAndSend(
                    "/topic/errors",
                    new MovementResponse(false, "Invalid movement data", 0, null)
            );
            return;
        }

        GameState gameState = gameController.getGame(movement.getGameId());

        try {
            if (gameState == null) {
                messagingTemplate.convertAndSend(
                        "/topic/errors",
                        new MovementResponse(false, "Game not found", 0, null)
                );
                return;
            }

            Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
            if (playerPosition == null) {
                messagingTemplate.convertAndSend(
                        "/topic/errors",
                        new MovementResponse(false, "Invalid movement data", 0, null)
                );
                return;
            }

            boolean success = gameState.movePlayer(
                    movement.getPlayerId(),
                    movement.getTicket(),
                    movement.getTargetPosition()
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + movement.getGameId(),
                    gameState
            );

            if (!success) {
                messagingTemplate.convertAndSend(
                        "/topic/errors",
                        new MovementResponse(
                                false,
                                "Invalid move",
                                gameState.getPlayerPosition(movement.getPlayerId()),
                                null
                        )
                );
                return;
            }

            messagingTemplate.convertAndSend(
                    "/topic/move-response",
                    new MovementResponse(
                            true,
                            "Movement successful",
                            gameState.getPlayerPosition(movement.getPlayerId()),
                            null
                    )
            );

        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/errors",
                    new MovementResponse(false, "Error: " + e.getMessage(), 0, null)
            );
        }
    }

    @MessageMapping("/lobby/create")
    @SendTo("/topic/lobby")
    public LobbyResponse handleCreateLobby(CreateLobbyMessage message) {
        try {
            User host = new User(
                    message.getUserId(),
                    message.getUserName(),
                    message.getPassword()
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
                    message.getUserName(),
                    message.getPassword()
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
}