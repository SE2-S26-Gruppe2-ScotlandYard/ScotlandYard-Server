package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceRejoinTest {

    @Test
    void testRejoinLobbySucceedsForExistingUser() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);

        Lobby result = service.rejoinLobby(lobby.getId(), host);
        assertNotNull(result);
        assertEquals(lobby.getId(), result.getId());
    }

    @Test
    void testRejoinLobbyThrowsForUnknownLobby() {
        LobbyService service = new LobbyService();
        User user = new User("1", "Stefan");
        assertThrows(IllegalArgumentException.class,
                () -> service.rejoinLobby("UNKNOWN_LOBBY_ID", user));
    }

    @Test
    void testRejoinLobbyAddsNewUserToLobby() {
        LobbyService service = new LobbyService();
        User host = new User("1", "Host");
        Lobby lobby = service.createLobby("TestLobby", host);
        User newUser = new User("2", "Stefan");

        Lobby result = service.rejoinLobby(lobby.getId(), newUser);
        assertTrue(result.getUsers().stream().anyMatch(u -> u.id().equals("2")));
    }
}