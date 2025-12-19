package ru.itis.scrabble.repositories;

import ru.itis.scrabble.models.User;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final ConcurrentHashMap<Long, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByUsername = new ConcurrentHashMap<>();
    private long nextId = 1;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(nextId++);
        }
        usersById.put(user.getId(), user);
        usersByUsername.put(user.getUsername(), user);
        return user;
    }

    @Override
    public void update(User user) {
        if (user.getId() != null && usersById.containsKey(user.getId())) {
            User existing = usersById.get(user.getId());
            if (!existing.getUsername().equals(user.getUsername())) {
                usersByUsername.remove(existing.getUsername());
                usersByUsername.put(user.getUsername(), user);
            }
            usersById.put(user.getId(), user);
        }
    }

    @Override
    public void deleteById(Long id) {
        User user = usersById.remove(id);
        if (user != null) {
            usersByUsername.remove(user.getUsername());
        }
    }

    @Override
    public User findById(Long id) {
        return usersById.get(id);
    }

    @Override
    public User findByUsername(String username) {
        return usersByUsername.get(username);
    }

    @Override
    public boolean existsById(Long id) {
        return usersById.containsKey(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }
}