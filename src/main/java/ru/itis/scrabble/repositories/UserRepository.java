package ru.itis.scrabble.repositories;

import ru.itis.scrabble.models.User;

public interface UserRepository {

    User save(User user);

    void update(User user);

    void deleteById(Long id);

    User findById(Long id);

    User findByEmail(String email);

    User findByUsername(String username);

    boolean existsById(Long id);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
