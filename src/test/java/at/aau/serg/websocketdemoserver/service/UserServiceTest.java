package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void testRegisterUserSuccess() {
        User user = userService.registerUser("Stefan", null);
        assertNotNull(user);
        assertNotNull(user.id());
        assertFalse(user.id().isBlank());
        assertEquals("Stefan", user.nickName());
    }

    @Test
    void testRegisterUserGeneratesUniqueIdsForDifferentNicknames() {
        User user1 = userService.registerUser("UserOne", null);
        User user2 = userService.registerUser("UserTwo", null);

        assertNotNull(user1.id());
        assertNotNull(user2.id());
        assertNotEquals(user1.id(), user2.id());
    }

    @Test
    void testRegisterUserThrowsExceptionIfNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(null, null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("", null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("   ", null));
    }

    @Test
    void testRegisterUserThrowsExceptionIfNameTooLong() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("toolongname", null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("123456789", null));
    }

    @Test
    void testRegisterUserThrowsExceptionIfNameHasInvalidChars() {
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("user!", null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("user 1", null));
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("user#name", null));
    }

    @Test
    void testRegisterUserSuccessWithMaxLength() {
        User user = userService.registerUser("abcd1234", null);
        assertNotNull(user);
        assertEquals("abcd1234", user.nickName());
    }

    @Test
    void testRegisterUserSuccessWithZeroAndUppercase() {
        User user = userService.registerUser("User0", null);
        assertNotNull(user);
        assertEquals("User0", user.nickName());
    }

    @Test
    void testRegisterUserSuccessWithDigits() {
        User user = userService.registerUser("player1", null);
        assertNotNull(user);
        assertEquals("player1", user.nickName());
    }

    @Test
    void testRegisterUserReturnsExistingUserWhenExistingUserIdIsKnown() {
        User first = userService.registerUser("Stefan", null);

        User again = userService.registerUser(null, first.id());

        assertEquals(first.id(), again.id());
        assertEquals(first.nickName(), again.nickName());
    }

    @Test
    void testRegisterUserWithKnownExistingUserIdIgnoresNicknameArgument() {
        User first = userService.registerUser("Stefan", null);

        User again = userService.registerUser("!!!invalid!!!", first.id());

        assertEquals(first.id(), again.id());
        assertEquals("Stefan", again.nickName());
    }

    @Test
    void testTwoDevicesWithSameNicknameGetDifferentIdsAndSecondIsRejected() {
        User deviceA = userService.registerUser("max", null);
        assertNotNull(deviceA.id());

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser("max", null));
    }

    @Test
    void testRegisterUserWithUnknownExistingUserIdFallsBackToRegisteringWithSameId() {
        String staleClientId = "client-had-this-id-before-restart";

        User user = userService.registerUser("Stefan", staleClientId);

        assertEquals(staleClientId, user.id());
        assertEquals("Stefan", user.nickName());
    }

    @Test
    void testRegisterUserWithUnknownExistingUserIdAndBlankNicknameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(null, "some-unknown-id"));
        assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser("", "some-unknown-id"));
    }

    @Test
    void testUnregisterUserByIdFreesNickname() {
        User user = userService.registerUser("Stefan", null);
        userService.unregisterUser(user.id());

        assertDoesNotThrow(() -> userService.registerUser("Stefan", null));
    }

    @Test
    void testUnregisterUserWithNullOrUnknownIdDoesNothing() {
        assertDoesNotThrow(() -> userService.unregisterUser(null));
        assertDoesNotThrow(() -> userService.unregisterUser(""));
        assertDoesNotThrow(() -> userService.unregisterUser("does-not-exist"));
    }

    @Test
    void testGetActiveUserCount() {
        userService.registerUser("User1", null);
        userService.registerUser("User2", null);
        userService.registerUser("User3", null);

        assertEquals(3, userService.getActiveUserCount());
    }

    @Test
    void testGetAllUsersReturnsAllRegisteredUsers() {
        User u1 = userService.registerUser("User1", null);
        User u2 = userService.registerUser("User2", null);

        Collection<User> all = userService.getAllUsers();

        assertEquals(2, all.size());
        assertTrue(all.contains(u1));
        assertTrue(all.contains(u2));
    }

    @Test
    void testGetAllUsersIsUnmodifiable() {
        userService.registerUser("User1", null);
        Collection<User> all = userService.getAllUsers();

        assertThrows(UnsupportedOperationException.class, all::clear);
    }

    @Test
    void testRenameUserSuccess() {
        User original = userService.registerUser("Stefan", null);

        User renamed = userService.renameUser(original.id(), "Steffi");

        assertEquals(original.id(), renamed.id());
        assertEquals("Steffi", renamed.nickName());
    }

    @Test
    void testRenameUserThrowsIfUserNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.renameUser("unknown-id", "NewName"));
    }

    @Test
    void testRenameUserThrowsIfNewNicknameInvalid() {
        User user = userService.registerUser("Stefan", null);

        assertThrows(IllegalArgumentException.class, () -> userService.renameUser(user.id(), null));
        assertThrows(IllegalArgumentException.class, () -> userService.renameUser(user.id(), ""));
        assertThrows(IllegalArgumentException.class, () -> userService.renameUser(user.id(), "toolongname"));
        assertThrows(IllegalArgumentException.class, () -> userService.renameUser(user.id(), "bad!name"));
    }

    @Test
    void testRenameUserThrowsIfNicknameTakenByAnotherActiveUser() {
        userService.registerUser("alice", null);
        User bob = userService.registerUser("bob", null);

        assertThrows(IllegalArgumentException.class, () -> userService.renameUser(bob.id(), "alice"));
    }

    @Test
    void testRenameUserToOwnCurrentNicknameDoesNotThrow() {
        User user = userService.registerUser("alice", null);

        assertDoesNotThrow(() -> userService.renameUser(user.id(), "alice"));
    }

    @Test
    void testRenameUserFreesOldNicknameForOtherUsers() {
        User alice = userService.registerUser("alice", null);
        userService.renameUser(alice.id(), "alicia");

        assertDoesNotThrow(() -> userService.registerUser("alice", null));
    }

    @Test
    void testRenameUserUpdatesGetAllUsers() {
        User user = userService.registerUser("Stefan", null);
        userService.renameUser(user.id(), "Steffi");

        Collection<User> all = userService.getAllUsers();
        assertEquals(1, all.size());
        assertEquals("Steffi", all.iterator().next().nickName());
    }
}