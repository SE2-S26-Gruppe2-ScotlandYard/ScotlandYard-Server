package at.aau.serg.websocketdemoserver.dtos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StartPositionDtosTest {

    // ── StartPositionRequest ───────────────────────────────────────────────

    @Test
    void testStartPositionRequestNoArgsConstructor() {
        StartPositionRequest req = new StartPositionRequest();
        assertThat(req.getGameId()).isNull();
        assertThat(req.getPlayerId()).isNull();
    }

    @Test
    void testStartPositionRequestAllArgsConstructor() {
        StartPositionRequest req = new StartPositionRequest("game-1", "player-1");
        assertThat(req.getGameId()).isEqualTo("game-1");
        assertThat(req.getPlayerId()).isEqualTo("player-1");
    }

    @Test
    void testStartPositionRequestSettersAndGetters() {
        StartPositionRequest req = new StartPositionRequest();
        req.setGameId("game-1");
        req.setPlayerId("player-1");

        assertThat(req.getGameId()).isEqualTo("game-1");
        assertThat(req.getPlayerId()).isEqualTo("player-1");
    }

    @Test
    void testStartPositionRequestEqualsAndHashCode() {
        StartPositionRequest r1 = new StartPositionRequest("game-1", "player-1");
        StartPositionRequest r2 = new StartPositionRequest("game-1", "player-1");
        StartPositionRequest r3 = new StartPositionRequest("game-2", "player-2");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
    }

    // ── StartPositionResponse ──────────────────────────────────────────────

    @Test
    void testStartPositionResponseNoArgsConstructor() {
        StartPositionResponse res = new StartPositionResponse();
        assertThat(res.getType()).isNull();
        assertThat(res.getGameId()).isNull();
        assertThat(res.getPlayerId()).isNull();
        assertThat(res.getStartPosition()).isNull();
        assertThat(res.getMessage()).isNull();
    }

    @Test
    void testStartPositionResponseAllArgsConstructor_success() {
        StartPositionResponse res = new StartPositionResponse(
                "START_POSITION_ASSIGNED", "game-1", "player-1", 42, null);

        assertThat(res.getType()).isEqualTo("START_POSITION_ASSIGNED");
        assertThat(res.getGameId()).isEqualTo("game-1");
        assertThat(res.getPlayerId()).isEqualTo("player-1");
        assertThat(res.getStartPosition()).isEqualTo(42);
        assertThat(res.getMessage()).isNull();
    }

    @Test
    void testStartPositionResponseAllArgsConstructor_error() {
        StartPositionResponse res = new StartPositionResponse(
                "ERROR", "game-1", "player-1", null, "Game not found");

        assertThat(res.getType()).isEqualTo("ERROR");
        assertThat(res.getStartPosition()).isNull();
        assertThat(res.getMessage()).isEqualTo("Game not found");
    }

    @Test
    void testStartPositionResponseSettersAndGetters() {
        StartPositionResponse res = new StartPositionResponse();
        res.setType("START_POSITION_ASSIGNED");
        res.setGameId("game-1");
        res.setPlayerId("player-1");
        res.setStartPosition(99);
        res.setMessage(null);

        assertThat(res.getType()).isEqualTo("START_POSITION_ASSIGNED");
        assertThat(res.getGameId()).isEqualTo("game-1");
        assertThat(res.getPlayerId()).isEqualTo("player-1");
        assertThat(res.getStartPosition()).isEqualTo(99);
        assertThat(res.getMessage()).isNull();
    }

    @Test
    void testStartPositionResponseEqualsAndHashCode() {
        StartPositionResponse r1 = new StartPositionResponse("START_POSITION_ASSIGNED", "g1", "p1", 42, null);
        StartPositionResponse r2 = new StartPositionResponse("START_POSITION_ASSIGNED", "g1", "p1", 42, null);
        StartPositionResponse r3 = new StartPositionResponse("ERROR", "g1", "p1", null, "fail");

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
    }
}

