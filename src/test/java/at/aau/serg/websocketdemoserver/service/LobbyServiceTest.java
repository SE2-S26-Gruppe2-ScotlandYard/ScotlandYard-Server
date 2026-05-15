package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.Role;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyServiceTest {

    // ── Bestehende Tests (unveraendert) ───────────────────────────────────

    @Test
    void testCreateLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Stefan");
        Lobby lobby = service.createLobby("TestLobby", host);

        assertNotNull(lobby);
        assertNotNull(lobby.getId());
        assertEquals("TestLobby", lobby.getName());
        assertEquals(host.id(), lobby.getHostId());
        assertEquals(1, lobby.getUsers().size());
        assertSame(lobby, service.getLobby(lobby.getId()));
    }

    @Test
    void testGetLobbyReturnsNullIfNotFound() {
        LobbyService service = new LobbyService();
        assertNull(service.getLobby("unknown-id"));
    }

    @Test
    void testJoinLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User user = new User("2", "Player");
        service.joinLobby(lobby.getId(), user);

        assertEquals(2, lobby.getUsers().size());
        assertTrue(lobby.getUsers().contains(user));
    }

    @Test
    void testJoinLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();
        User user = new User("2", "Player");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.joinLobby("missing-lobby", user)
        );
        assertEquals("Lobby not found", ex.getMessage());
    }

    @Test
    void testJoinLobbyFailsIfUserAlreadyInLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        assertThrows(IllegalStateException.class, () -> service.joinLobby(lobby.getId(), host));
    }

    @Test
    void testLeaveLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User user = new User("2", "Player");
        service.joinLobby(lobby.getId(), user);
        service.leaveLobby(lobby.getId(), user.id());

        assertFalse(lobby.getUsers().contains(user));
        assertEquals(1, lobby.getUsers().size());
    }

    @Test
    void testLeaveLobbyDeletesLobbyWhenLastPlayerLeaves() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        service.leaveLobby(lobby.getId(), host.id());

        assertNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testLeaveLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();
        assertThrows(IllegalArgumentException.class, () -> service.leaveLobby("missing-lobby", "1"));
    }

    @Test
    void testDeleteLobbyByHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        service.deleteLobby(lobby.getId(), host.id());

        assertNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testDeleteLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();
        assertThrows(IllegalArgumentException.class, () -> service.deleteLobby("missing-lobby", "1"));
    }

    @Test
    void testDeleteLobbyFailsForNonHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User other = new User("2", "Player");

        assertThrows(IllegalStateException.class, () -> service.deleteLobby(lobby.getId(), other.id()));
        assertNotNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testGetActiveLobbies() {
        LobbyService service = new LobbyService();
        assertTrue(service.getActiveLobbies().isEmpty());

        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        assertEquals(1, service.getActiveLobbies().size());
        assertTrue(service.getActiveLobbies().containsKey(lobby.getId()));
    }

    // ── NEUE Tests: kickPlayer ─────────────────────────────────────────────

    @Test
    void testKickPlayer_success() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User player = new User("2", "Player");
        service.joinLobby(lobby.getId(), player);

        Lobby result = service.kickPlayer(lobby.getId(), host.id(), player.id());

        assertEquals(1, result.getUsers().size());
        assertFalse(result.getUsers().contains(player));
    }

    @Test
    void testKickPlayer_failsForNonHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User player = new User("2", "Player");
        service.joinLobby(lobby.getId(), player);

        assertThrows(IllegalStateException.class,
                () -> service.kickPlayer(lobby.getId(), player.id(), host.id()));
    }

    @Test
    void testKickPlayer_failsIfLobbyNotFound() {
        LobbyService service = new LobbyService();
        assertThrows(IllegalArgumentException.class,
                () -> service.kickPlayer("missing", "1", "2"));
    }

    @Test
    void testKickPlayer_hostCannotKickThemself() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        assertThrows(IllegalStateException.class,
                () -> service.kickPlayer(lobby.getId(), host.id(), host.id()));
    }

    // ── NEUE Tests: setRole ────────────────────────────────────────────────

    @Test
    void testSetRole_playerSetsOwnRole() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        Lobby result = service.setRole(lobby.getId(), host.id(), host.id(), "MRX");

        assertEquals(Role.MRX, result.getSelectedRole(host.id()));
    }

    @Test
    void testSetRole_detective() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User player = new User("2", "Player");
        service.joinLobby(lobby.getId(), player);

        service.setRole(lobby.getId(), host.id(), host.id(), "MRX");
        Lobby result = service.setRole(lobby.getId(), player.id(), player.id(), "DETECTIVE");

        assertEquals(Role.DETECTIVE, result.getSelectedRole(player.id()));
    }

    @Test
    void testSetRole_failsWhenMrXAlreadyTaken() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User player = new User("2", "Player");
        service.joinLobby(lobby.getId(), player);

        service.setRole(lobby.getId(), host.id(), host.id(), "MRX");

        assertThrows(IllegalStateException.class,
                () -> service.setRole(lobby.getId(), player.id(), player.id(), "MRX"));
    }

    @Test
    void testSetRole_failsWhenSettingOtherPlayersRole() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User player = new User("2", "Player");
        service.joinLobby(lobby.getId(), player);

        // Host versucht Rolle von Player zu setzen → soll fehlschlagen
        assertThrows(IllegalStateException.class,
                () -> service.setRole(lobby.getId(), host.id(), player.id(), "DETECTIVE"));
    }

    @Test
    void testSetRole_failsIfLobbyNotFound() {
        LobbyService service = new LobbyService();
        assertThrows(IllegalArgumentException.class,
                () -> service.setRole("missing", "1", "1", "MRX"));
    }

    @Test
    void testSetRole_marksPlayerReadyAutomatically() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        assertFalse(lobby.getReadyStatus().get(host.id()), "Player should not be ready before selecting a role");

        service.setRole(lobby.getId(), host.id(), host.id(), "MRX");

        assertTrue(lobby.getReadyStatus().get(host.id()), "Player should be auto-marked ready after selecting a role");
    }
}