package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final int MAX_NICKNAME_LENGTH = 8;
    private static final Pattern VALID_NICKNAME_PATTERN = Pattern.compile("^[a-z0-9]+$");

    private final Map<String, User> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger userIdSequence = new AtomicInteger(1);

    public synchronized User registerUser(String nickName) {
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be empty");
        }

        String trimmed = nickName.trim();

        if (trimmed.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException("Nickname must be at most 8 characters long");
        }

        String lowerCaseName = trimmed.toLowerCase();

        if (!VALID_NICKNAME_PATTERN.matcher(lowerCaseName).matches()) {
            throw new IllegalArgumentException("Nickname may only contain letters (a-z) and digits (1-9)");
        }

        if (activeUsers.containsKey(lowerCaseName)) {
            return activeUsers.get(lowerCaseName); // Reconnect: return existing user
        }

        String generatedUserId = String.valueOf(userIdSequence.getAndIncrement());
        User newUser = new User(generatedUserId, trimmed);

        activeUsers.put(lowerCaseName, newUser);

        return newUser;
    }

    public synchronized void unregisterUser(String nickName) {
        if (nickName != null && !nickName.trim().isEmpty()) {
            activeUsers.remove(nickName.trim().toLowerCase());
        }
    }

    public int getActiveUserCount() {
        return activeUsers.size();
    }
}