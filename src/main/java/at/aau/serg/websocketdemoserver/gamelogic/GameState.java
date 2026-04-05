package at.aau.serg.websocketdemoserver.gamelogic;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import at.aau.serg.websocketdemoserver.gamelogic.board.Board;
import at.aau.serg.websocketdemoserver.gamelogic.player.Detective;
import at.aau.serg.websocketdemoserver.gamelogic.player.MrX;
import at.aau.serg.websocketdemoserver.gamelogic.player.Player;
import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import lombok.Getter;

public class GameState {
    @Getter
    private final String gameId;
    @Getter
    private final Board board;
    private final Map<String, Player> players = new HashMap<>();
    protected Map<String, Integer> playerPositions = new HashMap<>();
    private String mrXId;

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
                player = new MrX(user.name(), user.id());       // new Mr. X
                this.mrXId = user.id();
            } else {
                player = new Detective(user.name(), user.id()); // new Detective
            }

            players.put(user.id(), player);
        }

        // set start positions
        initializeStartPositions();
    }

    private void initializeStartPositions() {
        int[] startPositions = new int[] {13, 26, 29, 34, 50, 53, 91, 94, 103, 112, 117, 132, 138, 141, 155, 174, 197, 198};

        for (String playerId : players.keySet()) {
            int rnd = new Random().nextInt(startPositions.length);
            setPlayerPosition(playerId, startPositions[rnd]);
        }
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

    public Integer getPlayerPosition(String playerId) {
        return playerPositions.get(playerId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

}
