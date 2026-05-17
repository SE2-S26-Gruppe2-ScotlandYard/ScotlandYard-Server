package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserService {

    private final Map<String, User> activeUsers = new ConcurrentHashMap<>();
    private final AtomicInteger userIdSequence = new AtomicInteger(1);

    public synchronized User registerUser(String nickName) {
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be empty");
        }

        String lowerCaseName = nickName.trim().toLowerCase();

        if (activeUsers.containsKey(lowerCaseName)) {
            throw new IllegalArgumentException("Nickname already taken");
        }

        String generatedUserId = String.valueOf(userIdSequence.getAndIncrement());
        User newUser = new User(generatedUserId, nickName.trim());

        activeUsers.put(lowerCaseName, newUser);

        return newUser;
    }

    public synchronized void unregisterUser(String nickName) {
        if (nickName != null && !nickName.trim().isEmpty()) {
            activeUsers.remove(nickName.trim().toLowerCase());
        }
    }
}