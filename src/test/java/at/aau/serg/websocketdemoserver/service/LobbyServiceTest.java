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
        assertEquals("TestLobby", lobby.getName());
        assertEquals(host.id(), lobby.getHostId());
    }

    @Test
    void testJoinLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        User user = new User("2", "Player", "pass");
        service.joinLobby(lobby.getId(), user);

        assertTrue(lobby.getUsers().contains(user));
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
    void testDeleteLobbyFailsForNonHost() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host", "pass");
        Lobby lobby = service.createLobby("TestLobby", host);

        User other = new User("2", "Player", "pass");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            service.deleteLobby(lobby.getId(), other.id());
        });

        assertTrue(exception.getMessage().contains("host"));
    }

    @Test
    void debugFullFlow() {
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