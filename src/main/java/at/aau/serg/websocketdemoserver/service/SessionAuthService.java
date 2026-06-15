package at.aau.serg.websocketdemoserver.service;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionAuthService {

    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final Set<String> disconnectedUsers = ConcurrentHashMap.newKeySet();

    public void bindSession(String sessionId, String userId) {
        if (sessionId != null && userId != null) {
            sessionToUser.put(sessionId, userId);
            disconnectedUsers.remove(userId); // User is back
        }
    }

    public String getUserForSession(String sessionId) {
        if (sessionId == null) return null;
        return sessionToUser.get(sessionId);
    }

    /**
     * Called when STOMP session disconnects. Returns the userId that was bound
     * to that session, or null if none. Marks the user as disconnected.
     */
    public String unbindSession(String sessionId) {
        if (sessionId == null) return null;
        String userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            disconnectedUsers.add(userId);
        }
        return userId;
    }

    public boolean isUserDisconnected(String userId) {
        return userId != null && disconnectedUsers.contains(userId);
    }

    public Set<String> getDisconnectedUsers() {
        return Collections.unmodifiableSet(disconnectedUsers);
    }

    public boolean isAuthorized(String sessionId, String claimedUserId) {
        if (sessionId == null) return true;
        if (claimedUserId == null) return false;
        String boundUserId = sessionToUser.get(sessionId);
        if (boundUserId == null) return false;
        return claimedUserId.equals(boundUserId);
    }

    public int getSessionCount() {
        return sessionToUser.size();
    }
}