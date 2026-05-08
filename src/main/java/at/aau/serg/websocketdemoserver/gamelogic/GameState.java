package at.aau.serg.websocketdemoserver.gamelogic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import at.aau.serg.websocketdemoserver.gamelogic.board.Board;
import at.aau.serg.websocketdemoserver.gamelogic.board.Connection;
import at.aau.serg.websocketdemoserver.gamelogic.player.Detective;
import at.aau.serg.websocketdemoserver.gamelogic.player.MrX;
import at.aau.serg.websocketdemoserver.gamelogic.player.Player;
import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import at.aau.serg.websocketdemoserver.service.RoundController;
import lombok.Getter;

public class GameState {
    @Getter
    private final String gameId;
    @Getter
    private final Board board;
    @Getter
    private final RoundController roundController = new RoundController();
    private final Map<String, Player> players = new HashMap<>();
    protected Map<String, Integer> playerPositions = new HashMap<>();
    private String mrXId;
    public static final int MAX_ROUNDS = 22;
    private final Random RANDOM = new Random();     //NOSONAR not used in secure contexts

    public GameState (String gameId) {
        this.gameId = gameId;
        this.board = Board.getInstance();
    }

    public void initializeFromLobby(Lobby lobby) {
        if (!lobby.canStartGame()) {
            throw new IllegalStateException("Lobby is not ready to start the game");
        }

        for (User user : lobby.getUsers()) {
            Role role = lobby.getSelectedRole(user.id());

            Player player;
            if (role == Role.MRX) {
                player = new MrX(user);       // new Mr. X
                this.mrXId = user.id();
            } else {
                player = new Detective(user); // new Detective
            }

            // tell the RoundController which PlayerIDs belong to detectives
            Set<String> detectiveIds = players.keySet().stream()
                    .filter(id -> !id.equals(mrXId))
                    .collect(Collectors.toSet());
            roundController.initDetectives(detectiveIds);

            players.put(user.id(), player);
        }

        // set start positions
        initializeStartPositions();
    }

    private void initializeStartPositions() {
        int[] startPositions = new int[] {13, 26, 29, 34, 50, 53, 91, 94, 103, 112, 117, 132, 138, 141, 155, 174, 197, 198};

        for (String playerId : players.keySet()) {
            int rnd = RANDOM.nextInt(startPositions.length);
            setPlayerPosition(playerId, startPositions[rnd]);
        }
    }

    public boolean activateDoubleMove() {
        Player player = players.get(mrXId);

        if (!player.hasTicket(TicketType.DOUBLE)) {
            return false;
        }
        if (!roundController.isMrXTurn()) {
            return false;
        }
        if (roundController.isDoubleMoveActive()) {
            return false;
        }

        player.useTicket(TicketType.DOUBLE);
        roundController.activateDoubleMove();
        return true;
    }

    public void setPlayerPosition(String playerId, int position) {
        if (position < 1 || position > 199) {
            throw new IllegalArgumentException("Position must be between 1 and 199");
        }
        if (!players.containsKey(playerId)) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }

        playerPositions.put(playerId, position);
    }

    public Integer getMrXPosition() {
        return playerPositions.get(mrXId);
    }

    public Map<String, Integer> getDetectivePositions() {
        Map<String, Integer> detectivePositions = new HashMap<>();
        for (String playerId : players.keySet()) {
            if (!playerId.equals(mrXId)) {
                detectivePositions.put(playerId, playerPositions.get(playerId));
            }
        }

        return Collections.unmodifiableMap(detectivePositions);
    }

    public Integer getPlayerPosition(String playerId) {
        return playerPositions.get(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public int getCurrentRound() {
        return roundController.getCurrentRound().get();
    }

    public TurnType getCurrentPhase() {
        return roundController.getCurrentPhase();
    }

    public boolean movePlayer(String playerId, TicketType ticket, int newPosition) {
        try {
            if (!players.containsKey(playerId)) {   // player has to exist to move
                return false;
            }

            Integer currentPosition = playerPositions.get(playerId);
            if (currentPosition == null) {          // player has to be on the board to move
                return false;
            }

            if (isValidMove(playerId, ticket, currentPosition, newPosition)) {
                // apply move
                getPlayer(playerId).useTicket(ticket);
                setPlayerPosition(playerId, newPosition);

                //increment round/change phase
                if (getPlayer(playerId).isMrX()) {
                    roundController.recordMrXMove();
                } else {
                    roundController.recordDetectiveMove(playerId);
                }

                return true;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return false;
    }

    private boolean isValidMove(String playerId, TicketType ticket, int fromPosition, int toPosition) {
        if (getPlayer(playerId).hasTicket(ticket)) {
            Connection toCheck = new Connection(toPosition, ticket);
            return board.getStation(fromPosition).getConnections().contains(toCheck);
        }

        return false;
    }

    public boolean isCaught() {
        return getDetectivePositions().containsValue(getMrXPosition());
    }

    public GameResult checkGameResult() {
        if (isCaught()) {
            return GameResult.DETECTIVES_WIN;
        }
        if (getCurrentRound() > MAX_ROUNDS) {
            return GameResult.MRX_WINS;
        }
        return GameResult.ONGOING;
    }
}
