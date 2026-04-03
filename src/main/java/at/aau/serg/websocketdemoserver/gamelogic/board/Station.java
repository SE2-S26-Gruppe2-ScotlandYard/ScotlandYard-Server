package at.aau.serg.websocketdemoserver.gamelogic.board;

import java.util.Collections;
import java.util.List;

public class Station {
    private final int number;
    private final List<Connection> connections;

    public Station(int number, List<Connection> connections) {
        this.number = number;
        this.connections = connections;
    }

    public int getNumber() {
        return number;
    }

    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }
}