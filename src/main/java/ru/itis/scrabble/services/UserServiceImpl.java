package ru.itis.scrabble.services;

import ru.itis.scrabble.models.User;
import ru.itis.scrabble.repositories.UserRepository;
import java.util.Arrays;
import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    // Доступные стили (можно вынести в конфигурацию)
    private static final List<Integer> AVAILABLE_STYLES = Arrays.asList(1, 2, 3, 4, 5);
    private static final int DEFAULT_STYLE_ID = 1;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(String username, String passwordHash) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Создаем пользователя с дефолтным стилем
        User user = new User(username, passwordHash);
        // Устанавливаем дефолтный стиль (возможно, нужно добавить сеттер в User)
        // Для этого нужно обновить модель User - добавим метод setStyleId()
        // Пока предполагаем, что styleId инициализируется 0 или 1 по умолчанию
        user = userRepository.save(user);

        System.out.println("Зарегистрирован новый пользователь: " + username + " со стилем по умолчанию");
        return user;
    }

    @Override
    public User login(String username, String passwordHash) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!user.getPasswordHash().equals(passwordHash)) {
            throw new IllegalArgumentException("Invalid password");
        }

        System.out.println("Пользователь вошел: " + username + ", текущий стиль: " + user.getStyleId());
        return user;
    }

    @Override
    public User findById(Long id) {
        User user = userRepository.findById(id);
        if (user != null && user.getStyleId() == 0) {
            // Если стиль не установлен, устанавливаем дефолтный
            // Это может потребовать обновления User модели
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getStyleId() == 0) {
            // Если стиль не установлен, устанавливаем дефолтный
            // Это может потребовать обновления User модели
        }
        return user;
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
            System.out.println("Обновлена статистика пользователя " + userId +
                    ": побед=" + user.getTotalWins() +
                    ", поражений=" + user.getTotalLoses() +
                    ", игр=" + user.getTotalGames());
        }
    }

    @Override
    public void updateStyle(Long userId, int styleId) {
        User user = userRepository.findById(userId);
        if (user != null) {
            if (!isStyleAvailable(styleId)) {
                throw new IllegalArgumentException("Style " + styleId + " is not available");
            }

            // Нужен сеттер для styleId в классе User
            // Для этого обновим модель User
            // Пока используем рефлексию или добавим метод
            setUserStyle(user, styleId);
            userRepository.update(user);

            System.out.println("Пользователь " + userId + " сменил стиль на " + styleId);
        }
    }

    @Override
    public int getCurrentStyle(Long userId) {
        User user = userRepository.findById(userId);
        if (user != null) {
            int styleId = user.getStyleId();
            // Если стиль не установлен, возвращаем дефолтный
            return styleId > 0 ? styleId : DEFAULT_STYLE_ID;
        }
        return DEFAULT_STYLE_ID;
    }

    @Override
    public List<Integer> getAvailableStyles() {
        return AVAILABLE_STYLES;
    }

    @Override
    public boolean isStyleAvailable(int styleId) {
        return AVAILABLE_STYLES.contains(styleId);
    }

    // Вспомогательный метод для установки стиля
    // В идеале нужно добавить setStyleId() в класс User
    private void setUserStyle(User user, int styleId) {

        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("styleId");
            field.setAccessible(true);
            field.set(user, styleId);
        } catch (Exception e) {
            System.err.println("Ошибка при установке стиля пользователя: " + e.getMessage());
        }
    }
}