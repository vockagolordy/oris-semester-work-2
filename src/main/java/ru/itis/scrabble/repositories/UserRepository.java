package ru.itis.scrabble.repositories;

import ru.itis.scrabble.models.User;

public interface UserRepository {

    User save(User user);

    void update(User user);

    User findById(Long id);

    User findByUsername(String username);

    boolean existsByUsername(String username);
}
