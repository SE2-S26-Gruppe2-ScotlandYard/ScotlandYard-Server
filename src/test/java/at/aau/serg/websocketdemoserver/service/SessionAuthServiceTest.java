package at.aau.serg.websocketdemoserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionAuthServiceTest {

    private SessionAuthService service;

    @BeforeEach
    void setUp() {
        service = new SessionAuthService();
    }

    @Test
    void testBindAndGet() {
        service.bindSession("sess1", "user1");
        assertEquals("user1", service.getUserForSession("sess1"));
    }

    @Test
    void testBindWithNullSession() {
        service.bindSession(null, "user1");
        assertEquals(0, service.getSessionCount());
    }

    @Test
    void testBindWithNullUserId() {
        service.bindSession("sess1", null);
        assertEquals(0, service.getSessionCount());
    }

    @Test
    void testGetForNullSession() {
        assertNull(service.getUserForSession(null));
    }

    @Test
    void testGetForUnknownSession() {
        assertNull(service.getUserForSession("unknown"));
    }

    @Test
    void testUnbindSession() {
        service.bindSession("sess1", "user1");
        service.unbindSession("sess1");
        assertNull(service.getUserForSession("sess1"));
    }

    @Test
    void testUnbindNullSession() {
        service.unbindSession(null);
        assertEquals(0, service.getSessionCount());
    }

    @Test
    void testIsAuthorizedReturnsTrueForMatchingSession() {
        service.bindSession("sess1", "user1");
        assertTrue(service.isAuthorized("sess1", "user1"));
    }

    @Test
    void testIsAuthorizedReturnsFalseForMismatchedSession() {
        service.bindSession("sess1", "user1");
        assertFalse(service.isAuthorized("sess1", "user2"));
    }

    @Test
    void testIsAuthorizedReturnsTrueForNullSession() {
        assertTrue(service.isAuthorized(null, "user1"));
    }

    @Test
    void testIsAuthorizedReturnsFalseForNullUserId() {
        service.bindSession("sess1", "user1");
        assertFalse(service.isAuthorized("sess1", null));
    }

    @Test
    void testIsAuthorizedReturnsFalseForUnknownSession() {
        assertFalse(service.isAuthorized("unknownSess", "user1"));
    }

    @Test
    void testMultipleSessionsForDifferentUsers() {
        service.bindSession("sess1", "user1");
        service.bindSession("sess2", "user2");
        assertTrue(service.isAuthorized("sess1", "user1"));
        assertTrue(service.isAuthorized("sess2", "user2"));
        assertFalse(service.isAuthorized("sess1", "user2"));
        assertFalse(service.isAuthorized("sess2", "user1"));
    }

    @Test
    void testRebindSessionOverwrites() {
        service.bindSession("sess1", "user1");
        service.bindSession("sess1", "user2");
        assertEquals("user2", service.getUserForSession("sess1"));
    }

    @Test
    void testSessionCount() {
        assertEquals(0, service.getSessionCount());
        service.bindSession("sess1", "user1");
        service.bindSession("sess2", "user2");
        assertEquals(2, service.getSessionCount());
        service.unbindSession("sess1");
        assertEquals(1, service.getSessionCount());
    }
    @Test
    void testGetSessionForUser() {
        service.bindSession("sess1", "user1");
        assertEquals("sess1", service.getSessionForUser("user1"));
    }

    @Test
    void testGetSessionForUserNullUserId() {
        assertNull(service.getSessionForUser(null));
    }

    @Test
    void testGetSessionForUserUnknown() {
        assertNull(service.getSessionForUser("unknown"));
    }

    @Test
    void testUnbindSessionReturnsUserId() {
        service.bindSession("sess1", "user1");
        String userId = service.unbindSession("sess1");
        assertEquals("user1", userId);
    }

    @Test
    void testUnbindSessionMarksDisconnected() {
        service.bindSession("sess1", "user1");
        service.unbindSession("sess1");
        assertTrue(service.isUserDisconnected("user1"));
    }

    @Test
    void testUnbindSessionReturnsNullForUnknown() {
        assertNull(service.unbindSession("unknown"));
    }

    @Test
    void testBindSessionClearsDisconnected() {
        service.bindSession("sess1", "user1");
        service.unbindSession("sess1");
        assertTrue(service.isUserDisconnected("user1"));
        service.bindSession("sess2", "user1");
        assertFalse(service.isUserDisconnected("user1"));
    }
}
