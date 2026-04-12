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
import at.aau.serg.websocketdemoserver.websocket.broker.WebSocketBrokerController;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.Mockito.*;

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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketBrokerIntegrationTest {

    @LocalServerPort
    private int port;

    private final String WEBSOCKET_URI = "ws://localhost:%d/scotlandyard";
    private final String WEBSOCKET_TOPIC = "/topic/hello-response";
    private final String WEBSOCKET_TOPIC_OBJECT = "/topic/rcv-object";
    private final String WEBSOCKET_TOPIC_MOVE = "/topic/move-response";


    private GameController gameController;
    private String gameId;
    private String playerId;

    @BeforeEach
    void setUp() {
        gameId = "game1";
        playerId = "user1";

        GameState gameState = new GameState(gameId);
        gameController = GameController.getInstance();

        // create a simple lobby
        User user1 = new User(playerId, "User1", "pw");
        Lobby lobby = new Lobby(gameId, user1);
        lobby.addUser(user1);
        lobby.selectRole(playerId, Role.DETECTIVE);
        lobby.markPlayerReady("user1");

        User user2 = new User("user2", "Player2", "pw");
        lobby.addUser(user2);
        lobby.selectRole("user2", Role.MRX);
        lobby.markPlayerReady("user2");

        User user3 = new User("user3", "Player3", "pw");
        lobby.addUser(user3);
        lobby.selectRole("user3", Role.DETECTIVE);
        lobby.markPlayerReady("user3");

        gameState.initializeFromLobby(lobby);
        gameController.addGame(gameId, gameState);
    }

    @Test
    void testWebSocketMessageBroker() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingDeque<>(); // Queue of messages from the server.
        StompSession session = initStompSession(WEBSOCKET_TOPIC, new StringMessageConverter(), messages, String.class);

        // send a message to the server
        String message = "Test message";
        session.send("/app/hello", message);

        var expectedResponse = "echo from broker: " + message;
        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(expectedResponse);
    }

    @Test
    void testWebSocketMessageBrokerHandleObject() throws Exception {
        BlockingQueue<StompMessage> messages = new LinkedBlockingDeque<>(); // Queue of messages from the server.
        StompSession session = initStompSession(WEBSOCKET_TOPIC_OBJECT, new JacksonJsonMessageConverter(), messages, StompMessage.class);

        // send a message object to the server
        StompMessage message = new StompMessage("client", "Test Object Message");
        session.send("/app/object", message);

        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(message);
    }

    @Test
    void testHandleMove_SuccessfulMove() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE, new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        // set current position first
        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 2);

        // set TurnType to DETECTIVES
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(playerId);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isTrue();
        assertThat(actualResponse.getMessage()).isEqualTo("Movement successful");
    }

    @Test
    void testHandleMove_InvalidGameId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE, new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId("invalidGameId");
        movement.setPlayerId(playerId);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isFalse();
        assertThat(actualResponse.getMessage()).isEqualTo("Game not found");
    }

    @Test
    void testHandleMove_InvalidPlayerId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE, new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId("invalidPlayerId");
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid movement data");
    }

    @Test
    void testHandleMove_NullGameId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE, new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(null);
        movement.setPlayerId(playerId);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid movement data");
    }

    @Test
    void testHandleMove_NullPlayerId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE, new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(null);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid movement data");
    }

    @Test
    void testHandleMove_MultipleMoves() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE,
                new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(playerId);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(10);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);
        session.send("/app/move", movement);

        MovementResponse response1 = messages.poll(2, TimeUnit.SECONDS);
        MovementResponse response2 = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response1).isNotNull();
        assertThat(response2).isNotNull();
    }
    @Test
    void testHandleMove_InvalidTicket() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE,
                new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(playerId);
        movement.setTicket(null); // invalid
        movement.setTargetPosition(20);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void testHandleMove_RepeatedMoves() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_MOVE,
                new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        MovementMessage movement = new MovementMessage();
        movement.setGameId(gameId);
        movement.setPlayerId(playerId);
        movement.setTicket(TicketType.WALKING);
        movement.setTargetPosition(5);
        movement.setTimestamp(System.currentTimeMillis());

        session.send("/app/move", movement);
        session.send("/app/move", movement);

        MovementResponse r1 = messages.poll(2, TimeUnit.SECONDS);
        MovementResponse r2 = messages.poll(2, TimeUnit.SECONDS);

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
    }
    @Test
    void coverage_handleMove_nullMovement_direct() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        MovementResponse response = controller.handleMove(null);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void coverage_handleMove_invalidPlayerPosition_direct() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        MovementMessage msg = new MovementMessage();
        msg.setGameId("unknownGame");
        msg.setPlayerId("invalid");

        MovementResponse response = controller.handleMove(msg);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void coverage_handleMove_exception_direct() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        MovementMessage msg = new MovementMessage();
        msg.setGameId("1");
        msg.setPlayerId("p1");

        MovementResponse response = controller.handleMove(msg);

        assertThat(response).isNotNull();
    }
    @Test
    void coverage_handleMove_invalidMove_branch() {
        WebSocketBrokerController controller = new WebSocketBrokerController();

        MovementMessage msg = new MovementMessage();
        msg.setGameId("game1");
        msg.setPlayerId("user1");
        msg.setTargetPosition(-999); // force invalid move

        MovementResponse response = controller.handleMove(msg);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void coverage_handleMove_withMessagingTemplate() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = new WebSocketBrokerController(template);

        MovementMessage msg = new MovementMessage();
        msg.setGameId("game1");
        msg.setPlayerId("user1");

        controller.handleMove(msg);

        verify(template).convertAndSend(anyString(), any(Object.class));
    }
    /**
     * @return The Stomp session for the WebSocket connection (Stomp - WebSocket is comparable to HTTP - TCP).
     */
    public <T> StompSession initStompSession(String destination,
                                             MessageConverter messageConverter,
                                             BlockingQueue<T> queue,
                                             Class<T> expectedType) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(messageConverter);

        // connect client to the websocket server
        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {
                        })
                // wait 1 sec for the client to be connected
                .get(1, TimeUnit.SECONDS);

        // subscribes to the topic defined in WebSocketBrokerController
        // and adds received messages to WebSocketBrokerIntegrationTest#messages
        session.subscribe(destination, new StompFrameHandlerClientImpl<>(queue, expectedType));

        return session;
    }

}
