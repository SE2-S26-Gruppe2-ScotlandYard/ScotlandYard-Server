package at.aau.serg.websocketdemoserver.gamelogic.board;

import at.aau.serg.websocketdemoserver.gamelogic.player.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = Board.getInstance();
    }

    @Test
    void testBoardLoading() {
        Map<Integer, Station> stations = board.getStations();
        assertNotNull(stations);
        assertFalse(stations.isEmpty());
        assertEquals(199, stations.size());
    }

    @Test
    void testGetStation() {
        Station station = board.getStation(1);
        assertNotNull(station);
        assertEquals(1, station.getNumber());

        Station nonExistentStation = board.getStation(999);
        assertNull(nonExistentStation);
    }

    @Test
    void testStationConnections_Station1() {
        Station station1 = board.getStation(1);
        assertNotNull(station1);

        List<Connection> connections = station1.getConnections();
        assertNotNull(connections);
        assertEquals(5, connections.size());

        // Check first connection: {"to": 8, "transport": "WALKING"}
        Connection conn1 = connections.getFirst();
        assertEquals(8, conn1.to());
        assertEquals(TicketType.WALKING, conn1.transport());

        // Check third connection: {"to": 46, "transport": "ESCOOTER"}
        Connection conn3 = connections.get(2);
        assertEquals(46, conn3.to());
        assertEquals(TicketType.ESCOOTER, conn3.transport());

        // Check last connection: {"to": 46, "transport": "CARSHARING"}
        Connection conn5 = connections.get(4);
        assertEquals(46, conn5.to());
        assertEquals(TicketType.CARSHARING, conn5.transport());
    }
}
