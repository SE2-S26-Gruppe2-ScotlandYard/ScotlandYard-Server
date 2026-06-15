package at.aau.serg.websocketdemoserver.dtos.lobby;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LobbyDtosTests {

    @Test
    void testCreateLobbyMessageNoArgsConstructor() {
        CreateLobbyMessage message = new CreateLobbyMessage();

        assertThat(message.getLobbyName()).isNull();
        assertThat(message.getUserId()).isNull();
        assertThat(message.getNickName()).isNull();
    }

    @Test
    void testCreateLobbyMessageAllArgsConstructor() {
        CreateLobbyMessage message = new CreateLobbyMessage("TestLobby", "1", "Stefan");

        assertThat(message.getLobbyName()).isEqualTo("TestLobby");
        assertThat(message.getUserId()).isEqualTo("1");
        assertThat(message.getNickName()).isEqualTo("Stefan");
    }

    @Test
    void testCreateLobbyMessageSettersAndGetters() {
        CreateLobbyMessage message = new CreateLobbyMessage();

        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setNickName("Stefan");

        assertThat(message.getLobbyName()).isEqualTo("TestLobby");
        assertThat(message.getUserId()).isEqualTo("1");
        assertThat(message.getNickName()).isEqualTo("Stefan");
    }

    @Test
    void testCreateLobbyMessageEqualsAndHashCode() {
        CreateLobbyMessage m1 = new CreateLobbyMessage("TestLobby", "1", "Stefan");
        CreateLobbyMessage m2 = new CreateLobbyMessage("TestLobby", "1", "Stefan");
        CreateLobbyMessage m3 = new CreateLobbyMessage("OtherLobby", "2", "Anna");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testCreateLobbyMessageCanEqual() {
        CreateLobbyMessage m1 = new CreateLobbyMessage("TestLobby", "1", "Stefan");
        CreateLobbyMessage m2 = new CreateLobbyMessage("TestLobby", "1", "Stefan");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testCreateLobbyMessageEqualsSameObject() {
        CreateLobbyMessage message = new CreateLobbyMessage("TestLobby", "1", "Stefan");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testCreateLobbyMessageWithNullFields() {
        CreateLobbyMessage m1 = new CreateLobbyMessage(null, null, null);
        CreateLobbyMessage m2 = new CreateLobbyMessage(null, null, null);
        CreateLobbyMessage m3 = new CreateLobbyMessage("TestLobby", "1", "Stefan");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testJoinLobbyMessageNoArgsConstructor() {
        JoinLobbyMessage message = new JoinLobbyMessage();

        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getUserId()).isNull();
        assertThat(message.getNickName()).isNull();
    }

    @Test
    void testJoinLobbyMessageAllArgsConstructor() {
        JoinLobbyMessage message = new JoinLobbyMessage("lobby-123", "2", "Player");

        assertThat(message.getLobbyId()).isEqualTo("lobby-123");
        assertThat(message.getUserId()).isEqualTo("2");
        assertThat(message.getNickName()).isEqualTo("Player");
    }

    @Test
    void testJoinLobbyMessageSettersAndGetters() {
        JoinLobbyMessage message = new JoinLobbyMessage();

        message.setLobbyId("lobby-123");
        message.setUserId("2");
        message.setNickName("Player");

        assertThat(message.getLobbyId()).isEqualTo("lobby-123");
        assertThat(message.getUserId()).isEqualTo("2");
        assertThat(message.getNickName()).isEqualTo("Player");
    }

    @Test
    void testJoinLobbyMessageEqualsAndHashCode() {
        JoinLobbyMessage m1 = new JoinLobbyMessage("lobby-123", "2", "Player");
        JoinLobbyMessage m2 = new JoinLobbyMessage("lobby-123", "2", "Player");
        JoinLobbyMessage m3 = new JoinLobbyMessage("other-lobby", "3", "Other");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testJoinLobbyMessageCanEqual() {
        JoinLobbyMessage m1 = new JoinLobbyMessage("lobby-123", "2", "Player");
        JoinLobbyMessage m2 = new JoinLobbyMessage("lobby-123", "2", "Player");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testJoinLobbyMessageEqualsSameObject() {
        JoinLobbyMessage message = new JoinLobbyMessage("lobby-123", "2", "Player");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testJoinLobbyMessageWithNullFields() {
        JoinLobbyMessage m1 = new JoinLobbyMessage(null, null, null);
        JoinLobbyMessage m2 = new JoinLobbyMessage(null, null, null);
        JoinLobbyMessage m3 = new JoinLobbyMessage("lobby-123", "2", "Player");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testLeaveLobbyMessageNoArgsConstructor() {
        LeaveLobbyMessage message = new LeaveLobbyMessage();

        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getUserId()).isNull();
    }

    @Test
    void testLeaveLobbyMessageAllArgsConstructor() {
        LeaveLobbyMessage message = new LeaveLobbyMessage("lobby-456", "3");

        assertThat(message.getLobbyId()).isEqualTo("lobby-456");
        assertThat(message.getUserId()).isEqualTo("3");
    }

    @Test
    void testLeaveLobbyMessageSettersAndGetters() {
        LeaveLobbyMessage message = new LeaveLobbyMessage();

        message.setLobbyId("lobby-456");
        message.setUserId("3");

        assertThat(message.getLobbyId()).isEqualTo("lobby-456");
        assertThat(message.getUserId()).isEqualTo("3");
    }

    @Test
    void testLeaveLobbyMessageEqualsAndHashCode() {
        LeaveLobbyMessage m1 = new LeaveLobbyMessage("lobby-456", "3");
        LeaveLobbyMessage m2 = new LeaveLobbyMessage("lobby-456", "3");
        LeaveLobbyMessage m3 = new LeaveLobbyMessage("other-lobby", "4");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testLeaveLobbyMessageCanEqual() {
        LeaveLobbyMessage m1 = new LeaveLobbyMessage("lobby-456", "3");
        LeaveLobbyMessage m2 = new LeaveLobbyMessage("lobby-456", "3");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testLeaveLobbyMessageEqualsSameObject() {
        LeaveLobbyMessage message = new LeaveLobbyMessage("lobby-456", "3");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testLeaveLobbyMessageWithNullFields() {
        LeaveLobbyMessage m1 = new LeaveLobbyMessage(null, null);
        LeaveLobbyMessage m2 = new LeaveLobbyMessage(null, null);
        LeaveLobbyMessage m3 = new LeaveLobbyMessage("lobby-456", "3");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testDeleteLobbyMessageNoArgsConstructor() {
        DeleteLobbyMessage message = new DeleteLobbyMessage();

        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getRequesterId()).isNull();
    }

    @Test
    void testDeleteLobbyMessageAllArgsConstructor() {
        DeleteLobbyMessage message = new DeleteLobbyMessage("lobby-789", "host-1");

        assertThat(message.getLobbyId()).isEqualTo("lobby-789");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testDeleteLobbyMessageSettersAndGetters() {
        DeleteLobbyMessage message = new DeleteLobbyMessage();

        message.setLobbyId("lobby-789");
        message.setRequesterId("host-1");

        assertThat(message.getLobbyId()).isEqualTo("lobby-789");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testDeleteLobbyMessageEqualsAndHashCode() {
        DeleteLobbyMessage m1 = new DeleteLobbyMessage("lobby-789", "host-1");
        DeleteLobbyMessage m2 = new DeleteLobbyMessage("lobby-789", "host-1");
        DeleteLobbyMessage m3 = new DeleteLobbyMessage("other-lobby", "host-2");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testDeleteLobbyMessageCanEqual() {
        DeleteLobbyMessage m1 = new DeleteLobbyMessage("lobby-789", "host-1");
        DeleteLobbyMessage m2 = new DeleteLobbyMessage("lobby-789", "host-1");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testDeleteLobbyMessageEqualsSameObject() {
        DeleteLobbyMessage message = new DeleteLobbyMessage("lobby-789", "host-1");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testDeleteLobbyMessageWithNullFields() {
        DeleteLobbyMessage m1 = new DeleteLobbyMessage(null, null);
        DeleteLobbyMessage m2 = new DeleteLobbyMessage(null, null);
        DeleteLobbyMessage m3 = new DeleteLobbyMessage("lobby-789", "host-1");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testLobbyResponseNoArgsConstructor() {
        LobbyResponse response = new LobbyResponse();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getLobbyId()).isNull();
        assertThat(response.getLobby()).isNull();
    }

    @Test
    void testLobbyResponseAllArgsConstructor() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);

        LobbyResponse response = new LobbyResponse(true, "Lobby created successfully", lobby.getId(), lobby);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Lobby created successfully");
        assertThat(response.getLobbyId()).isEqualTo(lobby.getId());
        assertThat(response.getLobby()).isEqualTo(lobby);
    }

    @Test
    void testLobbyResponseSettersAndGetters() {
        LobbyResponse response = new LobbyResponse();

        response.setSuccess(true);
        response.setMessage("Lobby created successfully");
        response.setLobbyId("lobby-123");
        response.setLobby(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Lobby created successfully");
        assertThat(response.getLobbyId()).isEqualTo("lobby-123");
        assertThat(response.getLobby()).isNull();
    }

    @Test
    void testLobbyResponseEqualsAndHashCode() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);

        LobbyResponse r1 = new LobbyResponse(true, "ok", "lobby-1", lobby);
        LobbyResponse r2 = new LobbyResponse(true, "ok", "lobby-1", lobby);
        LobbyResponse r3 = new LobbyResponse(false, "error", "lobby-2", null);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1).isNotEqualTo(null);
        assertThat(r1).isNotEqualTo(new Object());
    }

    @Test
    void testLobbyResponseCanEqual() {
        LobbyResponse r1 = new LobbyResponse(true, "ok", "lobby-1", null);
        LobbyResponse r2 = new LobbyResponse(true, "ok", "lobby-1", null);

        assertThat(r1.canEqual(r2)).isTrue();
        assertThat(r1.canEqual(new Object())).isFalse();
    }

    @Test
    void testLobbyResponseEqualsSameObject() {
        LobbyResponse response = new LobbyResponse(true, "ok", "lobby-1", null);
        assertThat(response.equals(response)).isTrue();
    }

    @Test
    void testLobbyResponseWithNullFields() {
        LobbyResponse r1 = new LobbyResponse(false, null, null, null);
        LobbyResponse r2 = new LobbyResponse(false, null, null, null);
        LobbyResponse r3 = new LobbyResponse(true, "ok", "lobby-1", null);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testUserConnectMessageNoArgsConstructor() {
        UserConnectMessage message = new UserConnectMessage();
        assertThat(message.getNickName()).isNull();
    }

    @Test
    void testUserConnectMessageAllArgsConstructor() {
        UserConnectMessage message = new UserConnectMessage("Stefan", null);
        assertThat(message.getNickName()).isEqualTo("Stefan");
    }

    @Test
    void testUserConnectMessageSettersAndGetters() {
        UserConnectMessage message = new UserConnectMessage();
        message.setNickName("Stefan");
        assertThat(message.getNickName()).isEqualTo("Stefan");
    }

    @Test
    void testUserConnectResponseNoArgsConstructor() {
        UserConnectResponse response = new UserConnectResponse();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getUser()).isNull();
    }

    @Test
    void testUserConnectResponseAllArgsConstructor() {
        User user = new User("1", "Stefan");
        UserConnectResponse response = new UserConnectResponse(true, "Success", user);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getUser()).isEqualTo(user);
    }

    @Test
    void testUserConnectResponseSettersAndGetters() {
        UserConnectResponse response = new UserConnectResponse();
        User user = new User("1", "Stefan");

        response.setSuccess(true);
        response.setMessage("Success");
        response.setUser(user);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getUser()).isEqualTo(user);
    }
    // ── NEUE Tests: KickPlayerMessage ──────────────────────────────────────

    @Test
    void testKickPlayerMessageNoArgsConstructor() {
        KickPlayerMessage message = new KickPlayerMessage();
        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getRequesterId()).isNull();
        assertThat(message.getTargetUserId()).isNull();
    }

    @Test
    void testKickPlayerMessageAllArgsConstructor() {
        KickPlayerMessage message = new KickPlayerMessage("lobby-1", "host-1", "user-2");
        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
        assertThat(message.getTargetUserId()).isEqualTo("user-2");
    }

    @Test
    void testKickPlayerMessageSettersAndGetters() {
        KickPlayerMessage message = new KickPlayerMessage();
        message.setLobbyId("lobby-1");
        message.setRequesterId("host-1");
        message.setTargetUserId("user-2");

        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
        assertThat(message.getTargetUserId()).isEqualTo("user-2");
    }

    @Test
    void testKickPlayerMessageEqualsAndHashCode() {
        KickPlayerMessage m1 = new KickPlayerMessage("lobby-1", "host-1", "user-2");
        KickPlayerMessage m2 = new KickPlayerMessage("lobby-1", "host-1", "user-2");
        KickPlayerMessage m3 = new KickPlayerMessage("lobby-2", "host-2", "user-3");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
    }

    // ── NEUE Tests: SetRoleMessage ─────────────────────────────────────────

    @Test
    void testSetRoleMessageNoArgsConstructor() {
        SetRoleMessage message = new SetRoleMessage();
        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getRequesterId()).isNull();
        assertThat(message.getTargetUserId()).isNull();
        assertThat(message.getRole()).isNull();
    }

    @Test
    void testSetRoleMessageAllArgsConstructor() {
        SetRoleMessage message = new SetRoleMessage("lobby-1", "user-1", "user-1", "MRX");
        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("user-1");
        assertThat(message.getTargetUserId()).isEqualTo("user-1");
        assertThat(message.getRole()).isEqualTo("MRX");
    }

    @Test
    void testSetRoleMessageSettersAndGetters() {
        SetRoleMessage message = new SetRoleMessage();
        message.setLobbyId("lobby-1");
        message.setRequesterId("user-1");
        message.setTargetUserId("user-1");
        message.setRole("DETECTIVE");

        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("user-1");
        assertThat(message.getTargetUserId()).isEqualTo("user-1");
        assertThat(message.getRole()).isEqualTo("DETECTIVE");
    }

    @Test
    void testSetRoleMessageEqualsAndHashCode() {
        SetRoleMessage m1 = new SetRoleMessage("lobby-1", "user-1", "user-1", "MRX");
        SetRoleMessage m2 = new SetRoleMessage("lobby-1", "user-1", "user-1", "MRX");
        SetRoleMessage m3 = new SetRoleMessage("lobby-2", "user-2", "user-2", "DETECTIVE");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
    }

    // ── NEUE Tests: StartRoleSelectionMessage ──────────────────────────────

    @Test
    void testStartRoleSelectionMessageNoArgsConstructor() {
        StartRoleSelectionMessage message = new StartRoleSelectionMessage();
        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getRequesterId()).isNull();
    }

    @Test
    void testStartRoleSelectionMessageAllArgsConstructor() {
        StartRoleSelectionMessage message = new StartRoleSelectionMessage("lobby-1", "host-1");
        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testStartRoleSelectionMessageSettersAndGetters() {
        StartRoleSelectionMessage message = new StartRoleSelectionMessage();
        message.setLobbyId("lobby-1");
        message.setRequesterId("host-1");

        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testStartRoleSelectionMessageEqualsAndHashCode() {
        StartRoleSelectionMessage m1 = new StartRoleSelectionMessage("lobby-1", "host-1");
        StartRoleSelectionMessage m2 = new StartRoleSelectionMessage("lobby-1", "host-1");
        StartRoleSelectionMessage m3 = new StartRoleSelectionMessage("lobby-2", "host-2");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
    }

    // ── NEUE Tests: StartGameMessage ──────────────────────────────────────

    @Test
    void testStartGameMessageNoArgsConstructor() {
        StartGameMessage message = new StartGameMessage();
        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getRequesterId()).isNull();
    }

    @Test
    void testStartGameMessageAllArgsConstructor() {
        StartGameMessage message = new StartGameMessage("lobby-1", "host-1");
        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testStartGameMessageSettersAndGetters() {
        StartGameMessage message = new StartGameMessage();
        message.setLobbyId("lobby-1");
        message.setRequesterId("host-1");

        assertThat(message.getLobbyId()).isEqualTo("lobby-1");
        assertThat(message.getRequesterId()).isEqualTo("host-1");
    }

    @Test
    void testStartGameMessageEqualsAndHashCode() {
        StartGameMessage m1 = new StartGameMessage("lobby-1", "host-1");
        StartGameMessage m2 = new StartGameMessage("lobby-1", "host-1");
        StartGameMessage m3 = new StartGameMessage("lobby-2", "host-2");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
    }
}