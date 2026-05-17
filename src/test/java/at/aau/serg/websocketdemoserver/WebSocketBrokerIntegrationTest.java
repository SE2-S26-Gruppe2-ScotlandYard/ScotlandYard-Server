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
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WebSocketBrokerIntegrationTest {

    @LocalServerPort
    private int port;

    private final String WEBSOCKET_URI = "ws://localhost:%d/scotlandyard";
    private final String WEBSOCKET_TOPIC_HELLO = "/topic/hello-response";
    private final String WEBSOCKET_TOPIC_OBJECT = "/topic/rcv-object";

    private GameController gameController;
    private String gameId;
    private String playerId;

    @BeforeEach
    void setUp() {
        gameId = "game1";
        playerId = "user1";

        GameState gameState = new GameState(gameId);
        gameController = GameController.getInstance();

        User user1 = new User(playerId, "User1");
        Lobby lobby = new Lobby(gameId, user1);
        lobby.addUser(user1);
        lobby.selectRole(playerId, Role.DETECTIVE);
        lobby.markPlayerReady("user1");

        User user2 = new User("user2", "Player2");
        lobby.addUser(user2);
        lobby.selectRole("user2", Role.MRX);
        lobby.markPlayerReady("user2");

        User user3 = new User("user3", "Player3");
        lobby.addUser(user3);
        lobby.selectRole("user3", Role.DETECTIVE);
        lobby.markPlayerReady("user3");

        gameState.initializePlayersFromLobby(lobby);
        gameController.addGame(gameId, gameState);
    }

    private String getMoveTopic() {
        return "/topic/game/" + gameId + "/move-response";
    }

    private String getPlayerTopic(String userId) {
        return "/topic/player/" + userId;
    }

    @Test
    void testWebSocketMessageBroker() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_HELLO, new StringMessageConverter(), messages, String.class);
        session.send("/app/hello", "Test message");
        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo("echo from broker: Test message");
    }

    @Test
    void testWebSocketMessageBrokerHandleObject() throws Exception {
        BlockingQueue<StompMessage> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(WEBSOCKET_TOPIC_OBJECT, new JacksonJsonMessageConverter(), messages, StompMessage.class);
        StompMessage message = new StompMessage("client", "Test Object Message");
        session.send("/app/object", message);
        assertThat(messages.poll(1, TimeUnit.SECONDS)).isEqualTo(message);
    }

    @Test
    void testHandleMove_SuccessfulMove() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 1);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 8));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isTrue();
        assertThat(actualResponse.getMessage()).isEqualTo("Movement successful");
    }

    @Test
    void testHandleMove_InvalidGameId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/invalidGameId/move",
                createMovementMessage("invalidGameId", playerId, TicketType.WALKING, 20));

        MovementResponse actualResponse = messages.poll(2, TimeUnit.SECONDS);
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.isSuccess()).isFalse();
        assertThat(actualResponse.getMessage()).isEqualTo("Game not found");
    }

    @Test
    void testHandleMove_InvalidPlayerId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic("invalidPlayerId"), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "invalidPlayerId", TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid movement data");
    }

    @Test
    void testHandleMove_NullGameId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        // Fehler wird an das Player‑Topic gesendet, da der Controller die userId aus der Nachricht nimmt
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/null/move",
                createMovementMessage(null, playerId, TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        // Abhängig von der Implementierung kann "Game not found" oder eine andere Fehlermeldung kommen
        assertThat(response.getMessage()).isEqualTo("Game not found");
    }

    @Test
    void testHandleMove_NullPlayerId() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, null, TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("No player ID");
    }


    @Test
    void testHandleMove_RepeatedMoves() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        // Beide Detektive auf Startposition 1 setzen, Ziel 8 ist von 1 aus gültig.
        gameState.setPlayerPosition(playerId, 1);
        gameState.setPlayerPosition("user3", 1);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);
        gameState.getRoundController().addPendingDetectives("user3");

        // Erster Zug
        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 8));
        MovementResponse r1 = messages.poll(2, TimeUnit.SECONDS);
        assertThat(r1).isNotNull().extracting(MovementResponse::isSuccess).isEqualTo(true);

        // Zweiter Zug
        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user3", TicketType.WALKING, 8));
        MovementResponse r2 = messages.poll(2, TimeUnit.SECONDS);
        assertThat(r2).isNotNull().extracting(MovementResponse::isSuccess).isEqualTo(true);
    }

    @Test
    void testHandleMove_InvalidTicket() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, null, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
    }

    // ── Direkt‑Tests mit Mock (kein Spring Server) ────────────────

    @Test
    void coverage_handleMove_nullMovement_direct() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = new WebSocketBrokerController(template);
        controller.handleMove("game1", null);
        verify(template).convertAndSend(
                eq("/topic/game/game1/move-response"),
                argThat((MovementResponse r) -> !r.isSuccess())
        );
    }

    @Test
    void coverage_handleMove_withMessagingTemplate() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = new WebSocketBrokerController(template);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        controller.handleMove(gameId, createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        verify(template, atLeast(1)).convertAndSend(anyString(), any(Object.class));
    }

    // ── Weitere Spiel‑Tests mit korrekten Topics ──────────────────

    @Test
    void testHandleMove_detectiveDuringMrXPhase() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 2);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Not the detectives' turn");
    }

    @Test
    void testHandleMove_mrXDuringDetectivePhase() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic("user2"), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Not Mr. X's turn");
    }

    @Test
    void testHandleMove_detectiveAlreadyMoved() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition(playerId, 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives("user3");

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 20));

        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("already moved");
    }

    @Test
    void testActivateDouble_success() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.DOUBLE, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Double move ticket activated");
        assertThat(gameController.getGame(gameId).getRoundController().isDoubleMoveActive()).isTrue();
    }

    @Test
    void testActivateDouble_detectiveRequests() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic(playerId), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        gameController.getGame(gameId).getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameController.getGame(gameId).getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.DOUBLE, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Only Mr. X");
    }

    @Test
    void testActivateDouble_noMoreTickets() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic("user2"), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        for (int j = 10; j > 0; j--) {
            gameController.getGame(gameId).getPlayer("user2").useTicket(TicketType.DOUBLE);
        }

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.DOUBLE, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("No DOUBLE ticket");
    }

    @Test
    void testActivateDouble_whenAlreadyActive() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic("user2"), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        gameController.getGame(gameId).getRoundController().activateDoubleMove();

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.DOUBLE, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("already in use");
    }

    @Test
    void testCompleteDoubleMoveAction() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 2);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.DOUBLE, 20));
        MovementResponse activateResp = messages.poll(2, TimeUnit.SECONDS);
        assertThat(activateResp.isSuccess()).isTrue();

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.WALKING, 20));
        MovementResponse firstMove = messages.poll(2, TimeUnit.SECONDS);
        assertThat(firstMove.isSuccess()).isTrue();
        assertThat(firstMove.getMessage()).contains("1 move remaining");

        gameState.setPlayerPosition("user2", 20);
        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.WALKING, 2));
        MovementResponse secondMove = messages.poll(2, TimeUnit.SECONDS);
        assertThat(secondMove.isSuccess()).isTrue();
        assertThat(secondMove.getMessage()).doesNotContain("1 move remaining");
    }

    @Test
    void testAfterDoubleMove_detectivesCanMove() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 2);
        gameState.setPlayerPosition(playerId, 1);
        gameState.getRoundController().activateDoubleMove();
        gameState.movePlayer("user2", TicketType.WALKING, 20);
        gameState.movePlayer("user2", TicketType.WALKING, 2);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 8));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void testDoubleMove_detectiveCannotMoveBetweenMrXMoves() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getPlayerTopic("user2"), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 2);
        gameState.setPlayerPosition(playerId, 1);
        gameState.activateDoubleMove();
        gameState.movePlayer("user2", TicketType.WALKING, 20);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, "user2", TicketType.WALKING, 8));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Invalid move");
    }

    @Test
    void testHandleMove_detectiveLandsOnMrX() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 20);
        gameState.setPlayerPosition(playerId, 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Detectives win");
    }

    @Test
    void testDetectivesWin_broadcastsGameOverEvent() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = new WebSocketBrokerController(template);
        GameState gameState = gameController.getGame(gameId);
        gameState.setPlayerPosition("user2", 20);
        gameState.setPlayerPosition(playerId, 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        controller.handleMove(gameId, createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        verify(template).convertAndSend(
                eq("/topic/game/" + gameId + "/over"),
                eq("DETECTIVES_WIN")
        );
    }

    @Test
    void testHandleMove_pastMaxRoundsNotCaught() throws Exception {
        BlockingQueue<MovementResponse> q = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), q, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        while (gameState.getCurrentRound() <= GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        gameState.setPlayerPosition("user2", 10);
        gameState.setPlayerPosition(playerId, 20);
        gameState.setPlayerPosition("user3", 30);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 2));
        MovementResponse response = q.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Mr. X wins");
    }

    @Test
    void testMrxWins_broadcastsGameOverEvent() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBrokerController controller = new WebSocketBrokerController(template);
        GameState gameState = gameController.getGame(gameId);
        while (gameState.getCurrentRound() <= GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        gameState.setPlayerPosition("user2", 10);
        gameState.setPlayerPosition(playerId, 2);
        gameState.setPlayerPosition("user3", 50);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        controller.handleMove(gameId, createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        verify(template).convertAndSend(
                eq("/topic/game/" + gameId + "/over"),
                eq("MRX_WINS")
        );
    }

    @Test
    void testPastMaxRounds_detectiveCatchesMrX() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        while (gameState.getCurrentRound() <= GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        gameState.setPlayerPosition("user2", 20);
        gameState.setPlayerPosition(playerId, 2);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Detectives win");
    }

    @Test
    void testAtExactlyMaxRounds_gameStillOngoing() throws Exception {
        BlockingQueue<MovementResponse> messages = new LinkedBlockingDeque<>();
        StompSession session = initStompSession(getMoveTopic(), new JacksonJsonMessageConverter(), messages, MovementResponse.class);

        GameState gameState = gameController.getGame(gameId);
        while (gameState.getCurrentRound() < GameState.MAX_ROUNDS) {
            gameState.getRoundController().getCurrentRound().incrementAndGet();
        }
        assertThat(gameState.getCurrentRound()).isEqualTo(GameState.MAX_ROUNDS);

        gameState.setPlayerPosition("user2", 10);
        gameState.setPlayerPosition(playerId, 2);
        gameState.setPlayerPosition("user3", 50);
        gameState.getRoundController().setCurrentPhase(TurnType.DETECTIVES);
        gameState.getRoundController().addPendingDetectives(playerId);

        session.send("/app/game/" + gameId + "/move",
                createMovementMessage(gameId, playerId, TicketType.WALKING, 20));
        MovementResponse response = messages.poll(2, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Movement successful");
        assertThat(response.getMessage()).doesNotContain("GAME OVER");
    }

    // ========== Hilfsmethoden ==========

    public <T> StompSession initStompSession(String destination,
                                             MessageConverter messageConverter,
                                             BlockingQueue<T> queue,
                                             Class<T> expectedType) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(messageConverter);
        StompSession session = stompClient.connectAsync(String.format(WEBSOCKET_URI, port),
                        new StompSessionHandlerAdapter() {})
                .get(1, TimeUnit.SECONDS);
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