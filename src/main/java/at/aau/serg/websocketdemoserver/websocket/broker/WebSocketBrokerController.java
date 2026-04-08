package at.aau.serg.websocketdemoserver.websocket.broker;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.service.GameController;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;

import org.springframework.stereotype.Controller;


@Controller
public class WebSocketBrokerController {
    @MessageMapping("/hello")
    @SendTo("/topic/hello-response")
    public String handleHello(String text) {
        // TODO handle the messages here
        return "echo from broker: "+text;
    }
    @MessageMapping("/object")
    @SendTo("/topic/rcv-object")
    public StompMessage handleObject(StompMessage msg) {

       return msg;
    }

    private final GameController gameController = GameController.getInstance();

    @MessageMapping("/move")
    @SendTo("/topic/move-response")
    public MovementResponse handleMove(MovementMessage movement) {
        // validate gameId and playerId first
        if (movement.getGameId() == null || movement.getPlayerId() == null) {
            return new MovementResponse(false, "Invalid movement data", 0, null);
        }

        GameState gameState = gameController.getGame(movement.getGameId());

        try {
            if (gameState == null) {
                return new MovementResponse(false, "Game not found", 0, null);
            }

            // check if player exists
            Integer playerPosition = gameState.getPlayerPosition(movement.getPlayerId());
            if (playerPosition == null) {
                return new MovementResponse(false, "Invalid movement data", 0, null);
            }

            // move
            boolean success = gameState.movePlayer(
                    movement.getPlayerId(),
                    movement.getTicket(),
                    movement.getTargetPosition()
            );

            if (success) {
                return new MovementResponse(
                        true,
                        "Movement successful",
                        gameState.getPlayerPosition(movement.getPlayerId()),    // new position
                        null
                );
            } else {
                return new MovementResponse(false, "Invalid move", gameState.getPlayerPosition(movement.getPlayerId()), null);
            }

        } catch (Exception e) {
            assert gameState != null;
            return new MovementResponse(false, "Error: " + e.getMessage(), 0, null);
        }
    }

}
