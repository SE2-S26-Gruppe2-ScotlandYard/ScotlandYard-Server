package at.aau.serg.websocketdemoserver.gamelogic.board;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Board {
    private final Map<Integer, Station> stations = new HashMap<>();

    public Board() {
        loadBoard();
    }

    private void loadBoard() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("board.json")) {
            if (inputStream == null) {
                throw new RuntimeException("Cannot find board.json in resources folder.");
            }

            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, List<Connection>>> typeRef = new TypeReference<>() {};
            Map<String, List<Connection>> rawData = mapper.readValue(inputStream, typeRef);

            for (Map.Entry<String, List<Connection>> entry : rawData.entrySet()) {
                int stationNumber = Integer.parseInt(entry.getKey());
                List<Connection> connections = entry.getValue();
                stations.put(stationNumber, new Station(stationNumber, connections));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load or parse board.json", e);
        }
    }

    public Station getStation(int number) {
        return stations.get(number);
    }

    public Map<Integer, Station> getStations() {
        return Collections.unmodifiableMap(stations);
    }
}
