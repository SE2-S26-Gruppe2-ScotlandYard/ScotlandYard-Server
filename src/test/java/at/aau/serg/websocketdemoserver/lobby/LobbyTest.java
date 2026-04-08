package at.aau.serg.websocketdemoserver.lobby;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LobbyTest {

    private User host;
    private User user2;
    private User user3;
    private Lobby lobby;

    @BeforeEach
    void setUp() {
        host = new User("host1", "Host", "pw");
        user2 = new User("user2", "UserTwo", "pw");
        user3 = new User("user3", "UserThree", "pw");
        lobby = new Lobby("TestLobby", host);
    }

    @Test
    void testLobbyCreation() {
        assertNotNull(lobby.getId());
        assertEquals("TestLobby", lobby.getName());
        assertEquals(host.id(), lobby.getHostId());
        assertEquals(host, lobby.getHost());
        assertEquals(1, lobby.getUsers().size());
        assertFalse(lobby.isStarted());
    }

    @Test
    void testSetName() {
        lobby.setName("NewLobbyName");
        assertEquals("NewLobbyName", lobby.getName());
    }

    @Test
    void testAddUser() {
        lobby.addUser(user2);
        assertEquals(2, lobby.getUsers().size());
        assertTrue(lobby.getUsers().contains(user2));
    }

    @Test
    void testAddUser_LobbyFull() {
        lobby.addUser(new User("u2", "p2", "pw"));
        lobby.addUser(new User("u3", "p3", "pw"));
        lobby.addUser(new User("u4", "p4", "pw"));
        lobby.addUser(new User("u5", "p5", "pw"));
        lobby.addUser(new User("u6", "p6", "pw"));
        assertTrue(lobby.isFull());
        assertThrows(IllegalStateException.class, () -> lobby.addUser(new User("u7", "p7", "pw")));
    }

    @Test
    void testRemoveUser() {
        lobby.addUser(user2);
        lobby.removeUser(user2.id());
        assertEquals(1, lobby.getUsers().size());
        assertFalse(lobby.getUsers().contains(user2));
    }

    @Test
    void testRemoveUser_HostLeaves() {
        lobby.addUser(user2);
        lobby.removeUser(host.id());
        assertEquals(1, lobby.getUsers().size());
        assertEquals(user2.id(), lobby.getHostId()); // user2 should be the new host
    }

    @Test
    void testRemoveUser_LastUserLeaves() {
        lobby.removeUser(host.id());
        assertTrue(lobby.isEmpty());
    }

    @Test
    void testSetHost() {
        lobby.addUser(user2);
        lobby.setHostId(user2.id());
        assertEquals(user2.id(), lobby.getHostId());
    }

    @Test
    void testSetHost_UserNotInLobby() {
        assertThrows(IllegalArgumentException.class, () -> lobby.setHostId("nonexistentUser"));
    }

    @Test
    void testReadyAndRoleStatus_EmptyLobby() {
        Lobby emptyLobby = new Lobby("Empty", host);
        emptyLobby.removeUser(host.id());
        assertFalse(emptyLobby.areAllPlayersReady());
        assertFalse(emptyLobby.allPlayersHaveSelectedRole());
    }

    @Test
    void testMarkReady_UserNotInLobby() {
        // This should not throw an exception, just do nothing.
        assertDoesNotThrow(() -> lobby.markPlayerReady("nonexistentUser"));
        assertDoesNotThrow(() -> lobby.markPlayerNotReady("nonexistentUser"));
    }


    @Nested
    class GameFlowTests {

        @BeforeEach
        void addUsers() {
            lobby.addUser(user2);
            lobby.addUser(user3);
        }

        @Test
        void testReadySystem() {
            assertFalse(lobby.areAllPlayersReady());
            lobby.markPlayerReady(host.id());
            lobby.markPlayerReady(user2.id());
            assertFalse(lobby.areAllPlayersReady());
            lobby.markPlayerReady(user3.id());
            assertTrue(lobby.areAllPlayersReady());
            lobby.markPlayerNotReady(user2.id());
            assertFalse(lobby.areAllPlayersReady());
        }

        @Test
        void testRoleSelection() {
            assertTrue(lobby.selectRole(host.id(), Role.MRX));
            assertFalse(lobby.selectRole(user2.id(), Role.MRX)); // Should fail, MRX is taken
            assertTrue(lobby.selectRole(user2.id(), Role.DETECTIVE));
            assertTrue(lobby.selectRole(host.id(), Role.DETECTIVE)); // Host changes mind
            assertTrue(lobby.selectRole(user2.id(), Role.MRX)); // Now user2 can be MRX
        }

        @Test
        void testRoleSelection_UserNotInLobby() {
            assertFalse(lobby.selectRole("fakeUser", Role.DETECTIVE));
        }

        @Test
        void testCanStartGame_Conditions() {
            // Not enough players initially (in this nested setup)
            Lobby smallLobby = new Lobby("small", host);
            smallLobby.addUser(user2);

            // Mark ready
            smallLobby.markPlayerReady(host.id());
            smallLobby.markPlayerReady(user2.id());
            // Select roles
            smallLobby.selectRole(host.id(), Role.MRX);
            smallLobby.selectRole(user2.id(), Role.DETECTIVE);

            assertFalse(smallLobby.canStartGame(), "Should not be able to start with less than 3 players");

            // Add third player and check again
            smallLobby.addUser(user3);
            smallLobby.markPlayerReady(user3.id());
            smallLobby.selectRole(user3.id(), Role.DETECTIVE);
            assertTrue(smallLobby.canStartGame(), "Should be able to start with 3 ready players and roles set");
        }

        @Test
        void testStartGame_Success() {
            // Setup for a successful start
            lobby.markPlayerReady(host.id());
            lobby.markPlayerReady(user2.id());
            lobby.markPlayerReady(user3.id());
            lobby.selectRole(host.id(), Role.MRX);
            lobby.selectRole(user2.id(), Role.DETECTIVE);
            lobby.selectRole(user3.id(), Role.DETECTIVE);

            assertTrue(lobby.canStartGame());
            lobby.startGame();
            assertTrue(lobby.isStarted());
        }

        @Test
        void testStartGame_Failure() {
            // Not all players ready
            lobby.markPlayerReady(host.id());
            assertFalse(lobby.canStartGame());
            assertThrows(IllegalStateException.class, () -> lobby.startGame());
        }

        @Test
        void testAddUser_AfterGameStarted() {
            testStartGame_Success(); // Start the game
            assertThrows(IllegalStateException.class, () -> lobby.addUser(new User("u4", "p4", "pw")));
        }

        @Test
        void testGetSelectedRole_ReturnsSelected() {
            assertTrue(lobby.selectRole(host.id(), Role.MRX));          // assign role (Mr. X)
            assertEquals(Role.MRX, lobby.getSelectedRole(host.id()));   //check assigned role (Mr. X)
            assertTrue(lobby.selectRole(user2.id(), Role.DETECTIVE));          // assign role (Mr. X)
            assertEquals(Role.DETECTIVE, lobby.getSelectedRole(user2.id()));   //check assigned role (Mr. X)
        }
    }
}
