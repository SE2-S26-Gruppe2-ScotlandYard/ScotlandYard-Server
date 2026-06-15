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
    @Getter
    private String hostId;
    public static final int MAX_ROUNDS = 22;
    private final Random RANDOM = new Random();     //NOSONAR not used in secure contexts
    private final List<TicketType> mrXMoveHistory = new ArrayList<>(MAX_ROUNDS + 2);    // 2 = MAX_DOUBLE_TICKET
    private static final Set<Integer> REVEAL_ROUNDS = Set.of(3, 8, 13, 18);
    private final Map<Integer, Integer> mrXRevealedPositions = new HashMap<>();

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
        this.hostId = lobby.getHostId();
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

    /**
     * Removes a player from the running game (used when host kicks a disconnected player).
     * Returns the result type after kick: "MRX_KICKED" if MrX was kicked,
     * "TOO_FEW_PLAYERS" if less than 3 players remain, or "CONTINUE" otherwise.
     */
    public String kickPlayer(String requesterId, String targetId) {
        if (!requesterId.equals(hostId)) {
            throw new IllegalStateException("Only the host can kick players");
        }
        if (!players.containsKey(targetId)) {
            throw new IllegalArgumentException("Player not in game");
        }
        boolean wasMrX = targetId.equals(mrXId);
        players.remove(targetId);
        playerPositions.remove(targetId);
        if (!wasMrX) roundController.lockDetective(targetId);

        if (wasMrX) {
            return "MRX_KICKED";
        }
        if (players.size() < 2) {
            return "TOO_FEW_PLAYERS";
        }
        return "CONTINUE";
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

    /**
     * Assigns a start position to the given player.
     *
     * <p>Cheat/debug variant: when {@code selectedStartPosition} is non-null the
     * supplied value is used instead of the random fallback.  The position is
     * validated before it is applied:
     * <ul>
     *   <li>must be in range 1–199</li>
     *   <li>must not already be occupied by another player</li>
     * </ul>
     * If {@code selectedStartPosition} is {@code null} the call delegates to the
     * standard {@link #assignStartPosition(String)} method, preserving the
     * existing automatic behaviour for clients that do not send the field.
     *
     * @param playerId              the ID of the player to position
     * @param selectedStartPosition optional manually chosen position, or {@code null}
     * @return the assigned start position
     * @throws IllegalArgumentException if the player does not exist or the
     *                                  position is out of range
     * @throws IllegalStateException    if the position is already taken
     */
    public int assignStartPosition(String playerId, Integer selectedStartPosition) {
        // null → standard random fallback (backward compat)
        if (selectedStartPosition == null) {
            return assignStartPosition(playerId);
        }

        if (!players.containsKey(playerId)) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }

        // Player already has a position → return it unchanged
        Integer existingPosition = playerPositions.get(playerId);
        if (existingPosition != null) {
            return existingPosition;
        }

        // Range validation
        if (selectedStartPosition < 1 || selectedStartPosition > 199) {
            throw new IllegalArgumentException(
                    "Selected start position must be between 1 and 199, got: " + selectedStartPosition);
        }

        // Occupancy validation
        if (playerPositions.containsValue(selectedStartPosition)) {
            throw new IllegalStateException(
                    "Position " + selectedStartPosition + " is already taken by another player");
        }

        playerPositions.put(playerId, selectedStartPosition);
        return selectedStartPosition;
    }

    /**
     * Confirms a client-chosen start position (spinner confirm flow).
     *
     * <p>Unlike {@link #assignStartPosition(String, Integer)} this method always
     * writes the supplied position (it does not return an already-stored value
     * unchanged), so the client can correct its selection before the game
     * actually starts.  Validation rules:
     * <ul>
     *   <li>Player must exist.</li>
     *   <li>Position must be in range 1–199 (board stations).</li>
     *   <li>If the requested position is already occupied by a <em>different</em> player,
     *       a random free position is assigned instead (conflict-free fallback).</li>
     * </ul>
     *
     * @param playerId      ID of the confirming player
     * @param startPosition chosen start position (1–199)
     * @return the final confirmed position (may differ from {@code startPosition} if taken)
     * @throws IllegalArgumentException if player not found or position out of range
     * @throws IllegalStateException    if no free positions are available
     */
    public int confirmStartPosition(String playerId, int startPosition) {
        if (!players.containsKey(playerId)) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }
        if (startPosition < 1 || startPosition > 199) {
            throw new IllegalArgumentException(
                    "Start position must be between 1 and 199, got: " + startPosition);
        }

        // Check if another player already occupies this position → fall back to random free slot
        boolean taken = playerPositions.entrySet().stream()
                .anyMatch(e -> !e.getKey().equals(playerId) && e.getValue().equals(startPosition));

        if (taken) {
            List<Integer> available = new ArrayList<>();
            for (int i = 1; i <= 199; i++) available.add(i);
            // Remove positions held by other players (keep player's own slot free for reassignment)
            playerPositions.forEach((pid, pos) -> {
                if (!pid.equals(playerId)) available.remove(pos);
            });
            if (available.isEmpty()) {
                throw new IllegalStateException("No free start positions available");
            }
            Collections.shuffle(available, RANDOM);
            int fallback = available.getFirst();
            playerPositions.put(playerId, fallback);
            return fallback;
        }

        playerPositions.put(playerId, startPosition);
        return startPosition;
    }

    /**
     * Returns {@code true} when every registered player has a confirmed start position.
     * Used to detect when the board can become fully interactive.
     */
    public boolean allPlayersHaveStartPosition() {
        for (String playerId : players.keySet()) {
            if (!playerPositions.containsKey(playerId)) {
                return false;
            }
        }
        return !players.isEmpty();
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

    private void recordMrXMove(String playerId, TicketType ticket) {
        if (getPlayer(playerId).isMrX() && ticket != TicketType.DOUBLE) {
            mrXMoveHistory.add(ticket);
        }
    }

    private void recordRevealedPosition(String playerId) {
        if (getPlayer(playerId).isMrX()) {
            int round = getCurrentRound();
            if (REVEAL_ROUNDS.contains(round)) {
                mrXRevealedPositions.put(round, playerPositions.get(playerId));
            }
        }
    }

    private void incrementRoundOrChangePhase(String playerId) {
        if (getPlayer(playerId).isMrX()) {
            roundController.recordMrXMove();
        } else {
            roundController.recordDetectiveMove(playerId);
        }
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

    public Map<String, String> getPlayerNames() {
        Map<String, String> names = new HashMap<>();
        for (Map.Entry<String, Player> entry : players.entrySet()) {
            names.put(entry.getKey(), entry.getValue().getPlayerName());
        }
        return names;
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
                .toList();
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

    public Map<Integer, Integer> getMrXRevealedPositions() {
        return Collections.unmodifiableMap(mrXRevealedPositions);
    }

    public boolean movePlayer(String playerId, TicketType ticket, int newPosition) {
        try {
            if (!allPlayersHaveStartPosition()) {   // block moves until everyone is on the board
                return false;
            }
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

                recordMrXMove(playerId, ticket);
                recordRevealedPosition(playerId);

                setPlayerPosition(playerId, newPosition);

                incrementRoundOrChangePhase(playerId);
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

    public boolean hasValidMoves(String detectiveId) {
        Integer pos = playerPositions.get(detectiveId);
        if (pos == null) return true;   // no position yet = not placed, not stuck
        Player p = players.get(detectiveId);
        if (p == null) return false;
        return board.getStation(pos).getConnections().stream()
                .anyMatch(c -> p.hasTicket(c.transport()));
    }

    // Locks all pending detectives with no valid moves. Call this after each move
    // so that stuck detectives are never waited on again.
    public void lockStuckDetectives() {
        if (!roundController.isDetectiveTurn()) return;
        for (String id : new HashSet<>(roundController.getPendingDetectives())) {
            if (!hasValidMoves(id)) {
                roundController.lockDetective(id);
            }
        }
    }

    public GameResult checkGameResult() {
        if (isCaught()) {
            return GameResult.DETECTIVES_WIN;
        }
        if (getCurrentRound() > MAX_ROUNDS) {
            return GameResult.MRX_WINS;
        }
        if (roundController.allDetectivesLocked()) {
            return GameResult.MRX_WINS;
        }
        return GameResult.ONGOING;
    }
}
