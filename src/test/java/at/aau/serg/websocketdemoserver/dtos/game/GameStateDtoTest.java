package at.aau.serg.websocketdemoserver.dtos.game;

import at.aau.serg.websocketdemoserver.gamelogic.turn.TurnType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateDtoTest {

    private GameStateDto buildDto(String gameId, int round, TurnType phase) {
        boolean mrxPhase = phase == TurnType.MRX;
        boolean detPhase = phase == TurnType.DETECTIVES;
        return new GameStateDto(
                gameId,
                round,
                phase,
                mrxPhase,
                detPhase,
                Map.of("det1", 10),
                99,
                false,
                1,
                Map.of("det1", Map.of("WALKING", 10)),
                Map.of("BLACK", 5, "DOUBLE", 2),
                List.of("WALKING", "ESCOOTER"),
                Map.of(3, 42)
        );
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        GameStateDto dto = buildDto("g1", 2, TurnType.DETECTIVES);

        assertThat(dto.getGameId()).isEqualTo("g1");
        assertThat(dto.getCurrentRound()).isEqualTo(2);
        assertThat(dto.getCurrentPhase()).isEqualTo(TurnType.DETECTIVES);
        assertThat(dto.getDetectivePositions()).containsEntry("det1", 10);
        assertThat(dto.getMrXPosition()).isEqualTo(99);
        assertThat(dto.isDoubleMoveActive()).isFalse();
        assertThat(dto.getMrxMovesRemaining()).isEqualTo(1);
        assertThat(dto.getPlayerTickets()).containsKey("det1");
        assertThat(dto.getMrXSpecialTickets()).containsEntry("BLACK", 5);
        assertThat(dto.getMrXMoveHistory()).containsExactly("WALKING", "ESCOOTER");
        assertThat(dto.getMrXRevealedPositions()).containsEntry(3, 42);
    }

    @Test
    void testSetters() {
        GameStateDto dto = buildDto("g1", 1, TurnType.MRX);

        dto.setGameId("g2");
        dto.setCurrentRound(5);
        dto.setCurrentPhase(TurnType.DETECTIVES);
        dto.setDetectivePositions(Map.of("d2", 20));
        dto.setMrXPosition(55);
        dto.setDoubleMoveActive(true);
        dto.setMrxMovesRemaining(2);

        assertThat(dto.getGameId()).isEqualTo("g2");
        assertThat(dto.getCurrentRound()).isEqualTo(5);
        assertThat(dto.getCurrentPhase()).isEqualTo(TurnType.DETECTIVES);
        assertThat(dto.getDetectivePositions()).containsEntry("d2", 20);
        assertThat(dto.getMrXPosition()).isEqualTo(55);
        assertThat(dto.isDoubleMoveActive()).isTrue();
        assertThat(dto.getMrxMovesRemaining()).isEqualTo(2);
    }

    @Test
    void testEqualsAndHashCode() {
        GameStateDto dto1 = buildDto("g1", 1, TurnType.MRX);
        GameStateDto dto2 = buildDto("g1", 1, TurnType.MRX);
        GameStateDto dto3 = buildDto("g2", 3, TurnType.DETECTIVES);

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).hasSameHashCodeAs(dto2.hashCode());
        assertThat(dto1).isNotEqualTo(dto3);
    }

    @Test
    void testToString_containsGameId() {
        GameStateDto dto = buildDto("myGame", 1, TurnType.MRX);
        assertThat(dto.toString()).contains("myGame");
    }

    @Test
    void testNullMrXPosition() {
        GameStateDto dto = new GameStateDto("g1", 1, TurnType.MRX, true, false,
                Map.of(), null, false, 1, Map.of(), Map.of(), List.of(), Map.of());
        assertThat(dto.getMrXPosition()).isNull();
    }

    @Test
    void testIsMrXPhase_trueWhenMrXPhase() {
        GameStateDto dto = buildDto("g1", 1, TurnType.MRX);
        assertThat(dto.isMrXPhase()).isTrue();
        assertThat(dto.isDetectivesPhase()).isFalse();
    }

    @Test
    void testIsDetectivesPhase_trueWhenDetectivesPhase() {
        GameStateDto dto = buildDto("g1", 1, TurnType.DETECTIVES);
        assertThat(dto.isDetectivesPhase()).isTrue();
        assertThat(dto.isMrXPhase()).isFalse();
    }

    @Test
    void testNoArgsConstructorDefaultsPhaseFlags() {
        GameStateDto dto = new GameStateDto();
        assertThat(dto.isMrXPhase()).isFalse();
        assertThat(dto.isDetectivesPhase()).isFalse();
    }
}