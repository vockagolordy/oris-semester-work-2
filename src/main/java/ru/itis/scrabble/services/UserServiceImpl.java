package ru.itis.scrabble.services;

import org.mindrot.jbcrypt.BCrypt;
import ru.itis.scrabble.models.User;
import ru.itis.scrabble.repositories.UserRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Реализация сервиса управления пользователями с использованием BCrypt.
 * Библиотека BCrypt автоматически генерирует соль и внедряет её в итоговый хеш.
 */
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static final List<Integer> AVAILABLE_STYLES = Arrays.asList(1, 2, 3, 4, 5);
    private static final int DEFAULT_STYLE_ID = 1;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(String username, String password) {
        // 1. Валидация
        validateCredentials(username, password);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        // 2. Хеширование через BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // 3. Создание и сохранение
        User user = new User(username, hashedPassword);
        user.setStyleId(DEFAULT_STYLE_ID);

        return userRepository.save(user);
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        // 4. Проверка пароля (BCrypt сам извлечет соль из user.getPasswordHash())
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный пароль");
        }

        return user;
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public void updateGames(Long userId, boolean isWin) {
        User user = userRepository.findById(userId);
        if (user != null) {
            if (isWin) {
                user.addWin();
            } else {
                user.addLose();
            }
            userRepository.update(user);
        }
    }

    @Override
    public void updateStyle(Long userId, int styleId) {
        if (!isStyleAvailable(styleId)) {
            throw new IllegalArgumentException("Стиль #" + styleId + " недоступен");
        }

        User user = userRepository.findById(userId);
        if (user != null) {
            user.setStyleId(styleId);
            userRepository.update(user);
        }
    }

    @Override
    public int getCurrentStyle(Long userId) {
        User user = userRepository.findById(userId);
        return (user != null) ? user.getStyleId() : DEFAULT_STYLE_ID;
    }

    @Override
    public boolean isStyleAvailable(int styleId) {
        return AVAILABLE_STYLES.contains(styleId);
    }

    @Override
    public List<Integer> getAvailableStyles() {
        return AVAILABLE_STYLES;
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Логин должен содержать минимум 3 символа");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }
    }
}