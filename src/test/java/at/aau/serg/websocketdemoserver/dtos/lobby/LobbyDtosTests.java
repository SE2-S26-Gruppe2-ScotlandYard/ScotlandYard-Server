package at.aau.serg.websocketdemoserver.dtos.lobby;

import at.aau.serg.websocketdemoserver.lobby.Lobby;
import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyDtosTests {

    @Test
    void testCreateLobbyMessageNoArgsConstructorAndSetters() {
        CreateLobbyMessage message = new CreateLobbyMessage();

        message.setLobbyName("TestLobby");
        message.setUserId("1");
        message.setUserName("Stefan");
        message.setPassword("pass");

        assertEquals("TestLobby", message.getLobbyName());
        assertEquals("1", message.getUserId());
        assertEquals("Stefan", message.getUserName());
        assertEquals("pass", message.getPassword());
    }

    @Test
    void testJoinLobbyMessageNoArgsConstructorAndSetters() {
        JoinLobbyMessage message = new JoinLobbyMessage();

        message.setLobbyId("lobby-123");
        message.setUserId("2");
        message.setUserName("Player");
        message.setPassword("secret");

        assertEquals("lobby-123", message.getLobbyId());
        assertEquals("2", message.getUserId());
        assertEquals("Player", message.getUserName());
        assertEquals("secret", message.getPassword());
    }

    @Test
    void testLeaveLobbyMessageNoArgsConstructorAndSetters() {
        LeaveLobbyMessage message = new LeaveLobbyMessage();

        message.setLobbyId("lobby-456");
        message.setUserId("3");

        assertEquals("lobby-456", message.getLobbyId());
        assertEquals("3", message.getUserId());
    }

    @Test
    void testDeleteLobbyMessageNoArgsConstructorAndSetters() {
        DeleteLobbyMessage message = new DeleteLobbyMessage();

        message.setLobbyId("lobby-789");
        message.setRequesterId("host-1");

        assertEquals("lobby-789", message.getLobbyId());
        assertEquals("host-1", message.getRequesterId());
    }

    @Test
    void testLobbyResponseConstructorAndGetters() {
        User host = new User("1", "Host", "pass");
        Lobby lobby = new Lobby("TestLobby", host);

        LobbyResponse response = new LobbyResponse(
                true,
                "Lobby created successfully",
                lobby.getId(),
                lobby
        );

        assertTrue(response.isSuccess());
        assertEquals("Lobby created successfully", response.getMessage());
        assertEquals(lobby.getId(), response.getLobbyId());
        assertEquals(lobby, response.getLobby());
    }

    @Test
    void testLobbyResponseWithNullLobby() {
        LobbyResponse response = new LobbyResponse(
                false,
                "Lobby deleted",
                "lobby-999",
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("Lobby deleted", response.getMessage());
        assertEquals("lobby-999", response.getLobbyId());
        assertNull(response.getLobby());
    }
}