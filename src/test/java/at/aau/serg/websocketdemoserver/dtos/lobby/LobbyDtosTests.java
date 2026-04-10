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
        assertThat(message.getUserName()).isNull();
        assertThat(message.getPassword()).isNull();
    }

    @Test
    void testCreateLobbyMessageAllArgsConstructor() {
        CreateLobbyMessage message = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");

        assertThat(message.getLobbyName()).isEqualTo("TestLobby");
        assertThat(message.getUserId()).isEqualTo("1");
        assertThat(message.getUserName()).isEqualTo("Stefan");
        assertThat(message.getPassword()).isEqualTo("pass");
    }

    @Test
    void testCreateLobbyMessageSettersAndGetters() {
        CreateLobbyMessage message = new CreateLobbyMessage();

        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setUserName("Stefan");
        message.setPassword("pass");

        assertThat(message.getLobbyName()).isEqualTo("TestLobby");
        assertThat(message.getUserId()).isEqualTo("1");
        assertThat(message.getUserName()).isEqualTo("Stefan");
        assertThat(message.getPassword()).isEqualTo("pass");
    }

    @Test
    void testCreateLobbyMessageEqualsAndHashCode() {
        CreateLobbyMessage m1 = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");
        CreateLobbyMessage m2 = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");
        CreateLobbyMessage m3 = new CreateLobbyMessage("OtherLobby", "2", "Anna", "secret");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testCreateLobbyMessageCanEqual() {
        CreateLobbyMessage m1 = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");
        CreateLobbyMessage m2 = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testCreateLobbyMessageEqualsSameObject() {
        CreateLobbyMessage message = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testCreateLobbyMessageWithNullFields() {
        CreateLobbyMessage m1 = new CreateLobbyMessage(null, null, null, null);
        CreateLobbyMessage m2 = new CreateLobbyMessage(null, null, null, null);
        CreateLobbyMessage m3 = new CreateLobbyMessage("TestLobby", "1", "Stefan", "pass");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testJoinLobbyMessageNoArgsConstructor() {
        JoinLobbyMessage message = new JoinLobbyMessage();

        assertThat(message.getLobbyId()).isNull();
        assertThat(message.getUserId()).isNull();
        assertThat(message.getUserName()).isNull();
        assertThat(message.getPassword()).isNull();
    }

    @Test
    void testJoinLobbyMessageAllArgsConstructor() {
        JoinLobbyMessage message = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");

        assertThat(message.getLobbyId()).isEqualTo("lobby-123");
        assertThat(message.getUserId()).isEqualTo("2");
        assertThat(message.getUserName()).isEqualTo("Player");
        assertThat(message.getPassword()).isEqualTo("secret");
    }

    @Test
    void testJoinLobbyMessageSettersAndGetters() {
        JoinLobbyMessage message = new JoinLobbyMessage();

        message.setLobbyId("lobby-123");
        message.setUserId("2");
        message.setUserName("Player");
        message.setPassword("secret");

        assertThat(message.getLobbyId()).isEqualTo("lobby-123");
        assertThat(message.getUserId()).isEqualTo("2");
        assertThat(message.getUserName()).isEqualTo("Player");
        assertThat(message.getPassword()).isEqualTo("secret");
    }

    @Test
    void testJoinLobbyMessageEqualsAndHashCode() {
        JoinLobbyMessage m1 = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");
        JoinLobbyMessage m2 = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");
        JoinLobbyMessage m3 = new JoinLobbyMessage("other-lobby", "3", "Other", "pw");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    void testJoinLobbyMessageCanEqual() {
        JoinLobbyMessage m1 = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");
        JoinLobbyMessage m2 = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");

        assertThat(m1.canEqual(m2)).isTrue();
        assertThat(m1.canEqual(new Object())).isFalse();
    }

    @Test
    void testJoinLobbyMessageEqualsSameObject() {
        JoinLobbyMessage message = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");
        assertThat(message.equals(message)).isTrue();
    }

    @Test
    void testJoinLobbyMessageWithNullFields() {
        JoinLobbyMessage m1 = new JoinLobbyMessage(null, null, null, null);
        JoinLobbyMessage m2 = new JoinLobbyMessage(null, null, null, null);
        JoinLobbyMessage m3 = new JoinLobbyMessage("lobby-123", "2", "Player", "secret");

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
        User host = new User("1", "Host", "pass");
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
        User host = new User("1", "Host", "pass");
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
}