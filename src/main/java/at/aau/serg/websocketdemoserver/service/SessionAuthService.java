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
            disconnectedUsers.remove(userId);
        }
    }

    public String getUserForSession(String sessionId) {
        if (sessionId == null) return null;
        return sessionToUser.get(sessionId);
    }

    /**
     * Returns the sessionId currently bound to this userId, or null if none.
     */
    public String getSessionForUser(String userId) {
        if (userId == null) return null;
        for (Map.Entry<String, String> entry : sessionToUser.entrySet()) {
            if (userId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

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