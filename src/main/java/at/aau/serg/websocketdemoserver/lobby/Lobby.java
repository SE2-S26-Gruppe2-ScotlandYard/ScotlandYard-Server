package at.aau.serg.websocketdemoserver.lobby;

import java.util.*;

public class Lobby {

    private static final int MAX_PLAYERS = 6;
    private static final int MIN_PLAYERS = 3;

    private final String id;
    private String name;
    private boolean isStarted = false;
    private String hostId;

    // Using a Map for efficient user lookup by ID.
    private final Map<String, User> users = new LinkedHashMap<>();

    // New fields inspired by the example project.
    private final Map<String, Boolean> readyStatus = new HashMap<>();
    private final Map<String, Role> selectedRoles = new HashMap<>();

    public Lobby(String name, User host) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.hostId = host.id();
        addUser(host);
    }

    public void addUser(User user) {
        if (isFull() || isStarted) {
            throw new IllegalStateException("Lobby already started.");
        }
        users.put(user.id(), user);
        // A new user is never ready by default.
        readyStatus.put(user.id(), false);
        // A new user has not selected a role yet.
        selectedRoles.put(user.id(), Role.NONE);
    }

    public void removeUser(String userId) {
        users.remove(userId);
        readyStatus.remove(userId);
        selectedRoles.remove(userId);

        // If the host left and there are still users, assign a new host.
        if (Objects.equals(this.hostId, userId) && !users.isEmpty()) {
            // The next user in the insertion order becomes the new host.
            this.hostId = users.values().iterator().next().id();
        }
    }

    public List<User> getUsers() {
        return List.copyOf(users.values());
    }

    public boolean isFull() {
        return users.size() >= MAX_PLAYERS;
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public void markPlayerReady(String userId) {
        if (users.containsKey(userId)) {
            readyStatus.put(userId, true);
        }
    }

    public void markPlayerNotReady(String userId) {
        if (users.containsKey(userId)) {
            readyStatus.put(userId, false);
        }
    }

    public boolean areAllPlayersReady() {
        if (users.isEmpty()) {
            return false;
        }
        // Check if all users in the lobby have their ready status set to true.
        return readyStatus.values().stream().allMatch(Boolean::booleanValue);
    }

    public boolean selectRole(String userId, Role role) {
        if (!users.containsKey(userId)) {
            return false; // User not in lobby
        }

        // If trying to select MRX, check if it's already taken by another player.
        if (role == Role.MRX) {
            boolean isMrXAlreadyTaken = selectedRoles.entrySet().stream()
                    .anyMatch(entry -> entry.getValue() == Role.MRX && !entry.getKey().equals(userId));

            if (isMrXAlreadyTaken) {
                return false; // MRX role is already taken, selection fails.
            }
        }

        selectedRoles.put(userId, role);
        return true; // Selection was successful.
    }

    public Role getSelectedRole(String userId) {
        return selectedRoles.get(userId);
    }

    public boolean hasExactlyOneMrX() {
        return selectedRoles.values().stream()
                .filter(role -> role == Role.MRX)
                .count() == 1;
    }

    public boolean allPlayersHaveSelectedRole() {
        if (users.isEmpty()) {
            return false;
        }
        // Check if any user still has the NONE role.
        return selectedRoles.values().stream().noneMatch(role -> role == Role.NONE);
    }

    public void startGame() {
        if (!canStartGame()) {
            throw new IllegalStateException("Cannot start game: Not all conditions are met.");
        }
        this.isStarted = true;
    }

    public boolean canStartGame() {
        return hasEnoughPlayers() &&
                areAllPlayersReady() &&
                allPlayersHaveSelectedRole() &&
                hasExactlyOneMrX();
    }

    public boolean hasEnoughPlayers() {
        return users.size() >= MIN_PLAYERS;
    }

    //getters and setters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        if (!users.containsKey(hostId)) {
            throw new IllegalArgumentException("Cannot set host to a user who is not in the lobby.");
        }
        this.hostId = hostId;
    }

    public User getHost() {
        // This is a convenient way to get the full host object using the stored ID.
        return users.get(hostId);
    }
}
