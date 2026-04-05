package at.aau.serg.websocketdemoserver.gamelogic.board;

import java.util.Collections;
import java.util.List;

import lombok.Getter;

public class Station {
    @Getter
    private final int number;
    private final List<Connection> connections;

    public Station(int number, List<Connection> connections) {
        this.number = number;
        this.connections = connections;
    }

    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }
}