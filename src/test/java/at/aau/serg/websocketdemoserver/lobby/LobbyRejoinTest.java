package at.aau.serg.websocketdemoserver.lobby;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LobbyRejoinTest {

    @Test
    void testRejoinUserAddsUserBack() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        User user = new User("2", "Stefan");

        lobby.rejoinUser(user);
        assertTrue(lobby.getUsers().stream().anyMatch(u -> u.id().equals("2")));
    }

    @Test
    void testRejoinUserPreservesReadyStatus() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        User user = new User("2", "Stefan");

        lobby.addUser(user);
        lobby.markPlayerReady("2");

        lobby.rejoinUser(user);
        assertTrue(lobby.getReadyStatus().get("2"));
    }

    @Test
    void testRejoinUserPreservesRole() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        User user = new User("2", "Stefan");

        lobby.addUser(user);
        lobby.selectRole("2", Role.MRX);

        lobby.rejoinUser(user);
        assertEquals(Role.MRX, lobby.getSelectedRole("2"));
    }

    @Test
    void testRejoinUserSetsDefaultsForNewUser() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        User user = new User("2", "Stefan");

        lobby.rejoinUser(user);
        assertEquals(Role.NONE, lobby.getSelectedRole("2"));
    }

    @Test
    void testRejoinUserThrowsWhenLobbyIsFullAndUserNotInside() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        // Fill lobby to capacity (max 6)
        for (int i = 2; i <= 6; i++) {
            lobby.addUser(new User(String.valueOf(i), "User" + i));
        }
        User newUser = new User("99", "NewUser");
        assertThrows(IllegalStateException.class, () -> lobby.rejoinUser(newUser));
    }

    @Test
    void testRejoinUserSucceedsWhenLobbyIsFullButUserAlreadyInside() {
        User host = new User("1", "Host");
        Lobby lobby = new Lobby("TestLobby", host);
        User existing = new User("2", "Existing");
        lobby.addUser(existing);
        for (int i = 3; i <= 6; i++) {
            lobby.addUser(new User(String.valueOf(i), "User" + i));
        }
        // existing user should be able to rejoin even though lobby is full
        assertDoesNotThrow(() -> lobby.rejoinUser(existing));
    }
}