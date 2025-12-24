package ru.itis.scrabble.services;

import ru.itis.scrabble.models.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Демонстрационная реализация сервиса (Заглушка).
 * Работает в оперативной памяти, не требует БД и репозиториев.
 */
public class UserServiceImpl implements UserService {

    // Имитация базы данных в оперативной памяти
    private static final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private static final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(1);

    private static final List<Integer> AVAILABLE_STYLES = Arrays.asList(1, 2, 3, 4, 5);
    private static final int DEFAULT_STYLE_ID = 1;

    public UserServiceImpl(Object unusedRepository) {
        // Конструктор принимает Object, чтобы не ломать зависимости в местах вызова,
        // но репозиторий нам больше не нужен.
        System.out.println("UserService запущен в режиме демо");

        // Предзаполним одного тестового пользователя для удобства
        prefillMockData();
    }

    private void prefillMockData() {
        register("admin", "123456");
        register("player", "123456");
    }

    @Override
    public User register(String username, String password) {
        validateCredentials(username, password);

        if (usersByUsername.containsKey(username)) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        // Вместо хеширования BCrypt просто сохраняем пароль (для демо это безопасно)
        User user = new User(username, password);
        long id = idGenerator.getAndIncrement();
        user.setId(id);
        user.setStyleId(DEFAULT_STYLE_ID);

        // Сохраняем в наши Map-ы
        usersByUsername.put(username, user);
        usersById.put(id, user);

        System.out.println("Демо-регистрация успешна: " + username);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = usersByUsername.get(username);

        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        // Простая проверка пароля без BCrypt для скорости и надежности демо
        if (!password.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный пароль");
        }

        System.out.println("Демо-вход выполнен: " + username);
        return user;
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
    public void updateGames(Long userId, int gameState) {
        User user = usersById.get(userId);
        if (user != null) {
            switch (gameState) {
                case -1 -> user.addLose();
                case 0 -> user.addGame();
                case 1 -> user.addWin();
            }
        }
    }

    @Override
    public void updateStyle(Long userId, int styleId) {
        if (!isStyleAvailable(styleId)) {
            throw new IllegalArgumentException("Стиль #" + styleId + " недоступен");
        }

        User user = usersById.get(userId);
        if (user != null) {
            user.setStyleId(styleId);
        }
    }

    @Override
    public int getCurrentStyle(Long userId) {
        User user = usersById.get(userId);
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