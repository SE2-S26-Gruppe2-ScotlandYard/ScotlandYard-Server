package at.aau.serg.websocketdemoserver.lobby;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserRecord() {
        User user = new User("user123", "Alice", "password123");

        assertEquals("user123", user.id());
        assertEquals("Alice", user.name());
        assertEquals("password123", user.password());
    }
}
