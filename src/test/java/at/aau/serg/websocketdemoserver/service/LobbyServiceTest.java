package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyServiceTest {

    @Test
    void testCreateLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Stefan", "pass");

        Lobby lobby = service.createLobby("TestLobby", host);

        assertNotNull(lobby);
        assertNotNull(lobby.getId());
        assertEquals("TestLobby", lobby.getName());
        assertEquals(host.id(), lobby.getHostId());
        assertEquals(1, lobby.getUsers().size());
        assertEquals(host, lobby.getUsers().get(0));
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
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        User user = new User("2", "Player", "pass");
        service.joinLobby(lobby.getId(), user);

        assertEquals(2, lobby.getUsers().size());
        assertTrue(lobby.getUsers().contains(user));
    }

    @Test
    void testJoinLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();
        User user = new User("2", "Player", "pass");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.joinLobby("missing-lobby", user)
        );

        assertEquals("Lobby not found", exception.getMessage());
    }

    @Test
    void testJoinLobbyFailsIfUserAlreadyInLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.joinLobby(lobby.getId(), host)
        );

        assertEquals("User already in lobby", exception.getMessage());
    }

    @Test
    void testLeaveLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        User user = new User("2", "Player", "pass");
        service.joinLobby(lobby.getId(), user);

        service.leaveLobby(lobby.getId(), user.id());

        assertFalse(lobby.getUsers().contains(user));
        assertEquals(1, lobby.getUsers().size());
        assertNotNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testLeaveLobbyDeletesLobbyWhenLastPlayerLeaves() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        service.leaveLobby(lobby.getId(), host.id());

        assertNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testLeaveLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.leaveLobby("missing-lobby", "1")
        );

        assertEquals("Lobby not found", exception.getMessage());
    }

    @Test
    void testDeleteLobbyByHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        service.deleteLobby(lobby.getId(), host.id());

        assertNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testDeleteLobbyFailsIfLobbyDoesNotExist() {
        LobbyService service = new LobbyService();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteLobby("missing-lobby", "1")
        );

        assertEquals("Lobby not found", exception.getMessage());
    }

    @Test
    void testDeleteLobbyFailsForNonHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        User other = new User("2", "Player", "pass");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deleteLobby(lobby.getId(), other.id())
        );

        assertEquals("Only host can delete lobby", exception.getMessage());
        assertNotNull(service.getLobby(lobby.getId()));
    }

    @Test
    void testFullFlow() {
        LobbyService service = new LobbyService();

        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("DebugLobby", host);

        User user = new User("2", "Player", "pass");
        service.joinLobby(lobby.getId(), user);

        assertEquals(2, lobby.getUsers().size());

        service.leaveLobby(lobby.getId(), user.id());

        assertEquals(1, lobby.getUsers().size());

        service.deleteLobby(lobby.getId(), host.id());

        assertNull(service.getLobby(lobby.getId()));
    }
}