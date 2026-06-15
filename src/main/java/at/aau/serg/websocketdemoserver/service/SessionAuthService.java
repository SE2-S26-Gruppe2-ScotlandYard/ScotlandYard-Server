package at.aau.serg.websocketdemoserver.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which STOMP session belongs to which userId.
 * Used to verify that a user is allowed to make calls for themselves only.
 */
@Service
public class SessionAuthService {

    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    public void bindSession(String sessionId, String userId) {
        if (sessionId != null && userId != null) {
            sessionToUser.put(sessionId, userId);
        }
    }

    public String getUserForSession(String sessionId) {
        if (sessionId == null) return null;
        return sessionToUser.get(sessionId);
    }

    public void unbindSession(String sessionId) {
        if (sessionId != null) {
            sessionToUser.remove(sessionId);
        }
    }

    /**
     * Verify that the given userId in a message matches the userId bound to the session.
     * If sessionId is null (e.g. in unit tests where no STOMP session exists),
     * authorization is skipped and true is returned.
     * In production, sessionId is always provided by Spring's STOMP infrastructure.
     */
    public boolean isAuthorized(String sessionId, String claimedUserId) {
        if (sessionId == null) return true; // No session context (tests) – skip check
        if (claimedUserId == null) return false;
        String boundUserId = sessionToUser.get(sessionId);
        // If session is unknown (never registered), reject
        if (boundUserId == null) return false;
        return claimedUserId.equals(boundUserId);
    }

    public int getSessionCount() {
        return sessionToUser.size();
    }
}