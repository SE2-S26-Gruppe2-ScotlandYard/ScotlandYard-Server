package at.aau.serg.websocketdemoserver.gamelogic;

import java.util.*;
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
    private final List<TicketType> mrXMoveHistory = new ArrayList<>(MAX_ROUNDS + 2);    // 2 = MAX_DOUBLE_TICKET

    public GameState (String gameId) {
        this.gameId = gameId;
        this.board = Board.getInstance();
    }

    /**
     * Initializes players directly from the lobby's current user list and roles,
     * without enforcing canStartGame() conditions. Useful when the game is started
     * before all lobby preconditions (min. players, ready status) are fully met,
     * e.g. during development or testing.
     */
    public void initializePlayersFromLobby(Lobby lobby) {
        for (User user : lobby.getUsers()) {
            Role role = lobby.getSelectedRole(user.id());
            Player player;
            if (role == Role.MRX) {
                player = new MrX(user);
                this.mrXId = user.id();
            } else {
                player = new Detective(user);
            }
            players.put(user.id(), player);
        }
        Set<String> detectiveIds = players.keySet().stream()
                .filter(id -> !id.equals(mrXId))
                .collect(Collectors.toSet());
        roundController.initDetectives(detectiveIds);
    }

    public int assignStartPosition(String playerId) {
        if (!players.containsKey(playerId)) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }

        Integer existingPosition = playerPositions.get(playerId);
        if (existingPosition != null) {
            return existingPosition;
        }

        List<Integer> availablePositions = new ArrayList<>();
        for (int i = 1; i <= 199; i++) {
            availablePositions.add(i);
        }

        availablePositions.removeAll(playerPositions.values());

        if (availablePositions.isEmpty()) {
            throw new IllegalStateException("No free start positions available");
        }

        Collections.shuffle(availablePositions, RANDOM);
        int assignedPosition = availablePositions.getFirst();

        playerPositions.put(playerId, assignedPosition);
        return assignedPosition;
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

    public List<String> getMrXMoveHistory() {
        return mrXMoveHistory.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, Integer>> getPlayerTickets() {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        for (Map.Entry<String, Player> entry : players.entrySet()) {
            Map<String, Integer> ticketMap = new HashMap<>();
            for (Map.Entry<TicketType, Integer> t : entry.getValue().getTickets().entrySet()) {
                ticketMap.put(t.getKey().name(), t.getValue());
            }
            result.put(entry.getKey(), ticketMap);
        }
        return result;
    }

    public Map<String, Integer> getMrXSpecialTickets() {
        Player mrX = players.get(mrXId);
        if (mrX == null) return Collections.emptyMap();
        Map<String, Integer> result = new HashMap<>();
        Map<TicketType, Integer> tickets = mrX.getTickets();
        if (tickets.containsKey(TicketType.BLACK))
            result.put("BLACK", tickets.get(TicketType.BLACK));
        if (tickets.containsKey(TicketType.DOUBLE))
            result.put("DOUBLE", tickets.get(TicketType.DOUBLE));
        return result;
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
                if (getPlayer(playerId).isMrX() && ticket != TicketType.DOUBLE) {
                    mrXMoveHistory.add(ticket);
                }
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
        if (!getPlayer(playerId).hasTicket(ticket)) {
            return false;
        }

        if (ticket == TicketType.BLACK) {
            return board.getStation(fromPosition).getConnections().stream().anyMatch(c -> c.to() == toPosition);
        }

        Connection toCheck = new Connection(toPosition, ticket);
        return board.getStation(fromPosition).getConnections().contains(toCheck);
    }

    public boolean isCaught() {
        Integer mrXPos = getMrXPosition();
        if (mrXPos == null) return false;
        return getDetectivePositions().containsValue(mrXPos);
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
