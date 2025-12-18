package ru.itis.scrabble.services;

import ru.itis.scrabble.models.User;

public interface UserService {

    void updateStyles(Long userId, int styleId);

    void updateGames(Long userId, boolean isWin);

    void deleteById(Long id);

    User findById(Long id);

    User findByUsername(String username);

    boolean existsById(Long id);

    boolean existsByUsername(String username);
}
