package at.aau.serg.websocketdemoserver.gamelogic;

import java.util.HashMap;
import java.util.Map;

import at.aau.serg.websocketdemoserver.gamelogic.board.Board;
import at.aau.serg.websocketdemoserver.gamelogic.player.Player;

public class GameState {
    private final String gameId;
    private final Board board;
    private final Map<String, Player> players = new HashMap<>();
    protected Map<String, Integer> playerPositions = new HashMap<>();

    public GameState (String gameId) {
        this.gameId = gameId;
        this.board = Board.getInstance();
    }
}
