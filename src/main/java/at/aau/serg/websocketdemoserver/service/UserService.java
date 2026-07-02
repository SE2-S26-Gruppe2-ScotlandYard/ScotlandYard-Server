package at.aau.serg.websocketdemoserver.service;

import at.aau.serg.websocketdemoserver.lobby.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
public class UserService {

    private static final int MAX_NICKNAME_LENGTH = 8;
    private static final Pattern VALID_NICKNAME_PATTERN = Pattern.compile("^[a-z0-9]+$");

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> nicknameToUserId = new ConcurrentHashMap<>();

    public synchronized User registerUser(String nickName, String existingUserId) { // TODO: Fix Tests
        if (existingUserId != null && !existingUserId.isBlank()) {
            User existing = usersById.get(existingUserId);
            if (existing != null) {
                return existing;
            }
            /* If server no longer knows this id (e.g. restarted) fall through and re-register below,
            but keep the client's id so it stays stable instead of silently swapping to a new one. */
        }

        String validatedNickname = validateNickname(nickName);
        String userId = (existingUserId != null && !existingUserId.isBlank())
                ? existingUserId
                : UUID.randomUUID().toString();

        String previousOwnerId = nicknameToUserId.get(validatedNickname.toLowerCase());
        if (previousOwnerId != null && !previousOwnerId.equals(userId) && usersById.containsKey(previousOwnerId)) {
            throw new IllegalArgumentException("Nickname already taken");
        }

        User newUser = new User(userId, validatedNickname);
        usersById.put(userId, newUser);
        nicknameToUserId.put(validatedNickname.toLowerCase(), userId);
        return newUser;
    }

    private String validateNickname(String nickName) {
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
        return trimmed;
    }

    public synchronized void unregisterUser(String userId) {
        User removed = usersById.remove(userId);
        if (removed != null) {
            nicknameToUserId.remove(removed.nickName().toLowerCase(), userId);
        }
    }

    public synchronized User renameUser(String userId, String newNickName) {    // TODO: Testing
        User existing = usersById.get(userId);
        if (existing == null) {
            throw new IllegalArgumentException("User not found");
        }

        String validatedNickname = validateNickname(newNickName);

        String previousOwnerId = nicknameToUserId.get(validatedNickname.toLowerCase());
        if (previousOwnerId != null && !previousOwnerId.equals(userId) && usersById.containsKey(previousOwnerId)) {
            throw new IllegalArgumentException("Nickname already taken");
        }

        User renamed = new User(userId, validatedNickname);
        usersById.put(userId, renamed);
        nicknameToUserId.remove(existing.nickName().toLowerCase(), userId);
        nicknameToUserId.put(validatedNickname.toLowerCase(), userId);
        return renamed;
    }

    public int getActiveUserCount() {
        return usersById.size();
    }
}