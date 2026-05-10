package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void testRegisterUserSuccess() {
        User user = userService.registerUser("Stefan");
        assertNotNull(user);
        assertEquals("1", user.id());
        assertEquals("Stefan", user.nickName());
    }

    @Test
    void testRegisterUserIncrementsId() {
        User user1 = userService.registerUser("UserOne");
        User user2 = userService.registerUser("UserTwo");

        assertEquals("1", user1.id());
        assertEquals("2", user2.id());
    }

    @Test
    void testRegisterUserThrowsExceptionIfNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(""));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("   "));
    }

    @Test
    void testRegisterUserThrowsExceptionIfNameTakenCaseInsensitive() {
        userService.registerUser("Stefan");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("stefan"));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("STEFAN"));
    }

    @Test
    void testUnregisterUserFreesName() {
        userService.registerUser("Stefan");
        userService.unregisterUser("Stefan");

        assertDoesNotThrow(() -> userService.registerUser("Stefan"));
    }

    @Test
    void testUnregisterUserWithNullOrEmptyDoesNothing() {
        assertDoesNotThrow(() -> userService.unregisterUser(null));
        assertDoesNotThrow(() -> userService.unregisterUser(""));
        assertDoesNotThrow(() -> userService.unregisterUser("   "));
    }
}