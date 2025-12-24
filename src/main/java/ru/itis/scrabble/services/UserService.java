package ru.itis.scrabble.services;

import ru.itis.scrabble.models.User;
import java.util.List;

public interface UserService {
    User register(String username, String passwordHash);
    User login(String username, String passwordHash);
    User findById(Long id);
    User findByUsername(String username);
    void updateGames(Long userId, int gameState);

    void updateStyle(Long userId, int styleId);
    int getCurrentStyle(Long userId);
    List<Integer> getAvailableStyles();
    boolean isStyleAvailable(int styleId);
}