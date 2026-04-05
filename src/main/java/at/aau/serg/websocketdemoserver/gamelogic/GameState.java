package at.aau.serg.websocketdemoserver.gamelogic;

import at.aau.serg.websocketdemoserver.gamelogic.board.Board;
import at.aau.serg.websocketdemoserver.gamelogic.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameState {

    private final Board board;
    private final List<Player> players;

    // Predefined start positions for players
    private static final List<Integer> START_POSITIONS = new ArrayList<>(Arrays.asList(
            13, 26, 29, 34, 50, 53, 91, 94, 103, 112, 117, 132, 138, 141, 155, 174, 197, 198
    ));

    public GameState(List<Player> players, Board board) {
        this.players = players;
        this.board = board;
        assignStartPositions();
    }

    private void assignStartPositions() {

        List<Integer> shuffledPositions = new ArrayList<>(START_POSITIONS);
        Collections.shuffle(shuffledPositions);

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setCurrentPosition(shuffledPositions.get(i));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Board getBoard() {
        return board;
    }
}
