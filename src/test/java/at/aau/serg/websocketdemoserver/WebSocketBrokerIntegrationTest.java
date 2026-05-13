package at.aau.serg.websocketdemoserver;

import at.aau.serg.websocketdemoserver.dtos.StompMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementMessage;
import at.aau.serg.websocketdemoserver.dtos.movement.MovementResponse;
import at.aau.serg.websocketdemoserver.gamelogic.GameState;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.GameController;
import at.aau.serg.websocketdemoserver.websocket.StompFrameHandlerClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WebSocketBrokerIntegrationTest {

    @LocalServerPort
    private int port;

    private final String WEBSOCKET_URI = "ws://localhost:%d/scotlandyard";
    private final String WEBSOCKET_TOPIC = "/topic/hello-response";
    private final String WEBSOCKET_TOPIC_OBJECT = "/topic/rcv-object";

    private GameController gameController;
    private String gameId;
    private String playerId;
    private String mrXId;

    private String getMovementTopic() {
        return "/topic/game/" + gameId + "/move-response";
    }

    private String getPrivateTopic(String pId) {
        return "/topic/player/" + pId;
    }

    @BeforeEach
    void setUp() {
        gameId = "game1";
        playerId = "user1";
        mrXId = "user2";

        gameController = GameController.getInstance();

        User user1 = new User(playerId, "User1");
        User user2 = new User(mrXId, "MrX");
        User user3 = new User("user3", "Player3");

        Lobby lobby = new Lobby(gameId, user1);
        lobby.addUser(user1);
        lobby.addUser(user2);
        lobby.addUser(user3);

        lobby.selectRole(playerId, Role.DETECTIVE);
        lobby.selectRole(mrXId, Role.MRX);
        lobby.selectRole("user3", Role.DETECTIVE);

        lobby.markPlayerReady(playerId);
        lobby.markPlayerReady(mrXId);
        lobby.markPlayerReady("user3");

        GameState gameState = new GameState(gameId);
        gameState.initializeFromLobby(lobby);
        gameController.addGame(gameId, gameState);
    }

    @Test
    void testWebSocketMessageBroker() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC, new StringMessageConverter(), messages, String.class);

        String message = "Test message";
        session.send("/app/hello", message);

        assertThat(messages.poll(2, TimeUnit.SECONDS)).isEqualTo("echo from broker: " + message);
    }

    @Test
    void testHandleMove_SuccessfulMove() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMovementTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 1);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        // Move von Station 1 zu 8 (Walking)
        session.send("/app/game/" + gameId + "/move", createMovementMessage(gameId, playerId, TicketType.WALKING, 8));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isTrue();
    }

    @Test
    void testHandleMove_WrongTurnError() throws Exception {
        // Detective versucht zu ziehen, während MrX dran ist
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPrivateTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.getRoundController().setCurrentPhase(TurnType.MRX);

        session.send("/app/game/" + gameId + "/move", createMovementMessage(gameId, playerId, TicketType.WALKING, 8));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isFalse();
        assertThat(actualResponse.getMessage()).containsIgnoringCase("turn");
    }

    @Test
    void testHandleMove_DoubleMoveActivation() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMovementTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.getRoundController().setCurrentPhase(TurnType.MRX);

        // MrX sendet Double-Ticket
        session.send("/app/game/" + gameId + "/move", createMovementMessage(gameId, mrXId, TicketType.DOUBLE, 0));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isTrue();
        assertThat(actualResponse.getMessage()).containsIgnoringCase("activated");
    }

    @Test
    void testHandleMove_InvalidGameId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPrivateTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/NON_EXISTENT/move", createMovementMessage("NON_EXISTENT", playerId, TicketType.WALKING, 10));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isFalse();
        assertThat(actualResponse.getMessage()).containsIgnoringCase("not found");
    }

    public <T> StompSession initStompSession(String destination,
                                             MessageConverter messageConverter,
                                             BlockingQueue<T> queue,
                                             Class<T> expectedType) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(messageConverter);

        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {
                        })
                .get(2, TimeUnit.SECONDS);

        session.subscribe(destination, new StompFrameHandlerClientImpl<>(queue, expectedType));
        return session;
    }

    private static MovementMessage createMovementMessage(String gameId, String playerId, TicketType ticket, int targetPosition) {
        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(playerId);
        movement.setTicket(ticket);
        movement.setTargetPosition(targetPosition);
        movement.setTimestamp(System.currentTimeMillis());
        return movement;
    }
}