package ru.itis.scrabble.models;

/**
 * Модель пользователя.
 * <p>
 * Содержит основные данные профиля пользователя, а также статистику игр.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code id} - уникальный идентификатор пользователя.
 *         Генерируется на уровне БД, при создании объекта может быть {@code null}</li>
 *     <li>{@code email} - электронная почта пользователя</li>
 *     <li>{@code username} - имя пользователя</li>
 *     <li>{@code passwordHash} - хэш пароля пользователя</li>
 *     <li>{@code totalWins} - количество выигранных игр</li>
 *     <li>{@code totalLoses} - количество проигранных игр</li>
 *     <li>{@code totalGames} - количество всех игр</li>
 *     <li>{@code styleId} - идентификатор стиля оформления</li>
 * </ul>
 */
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private int totalWins;
    private int totalLoses;
    private int totalGames;
    private int styleId;

    public User(Long id, String username, String passwordHash, int totalWins, int totalLoses, int totalGames) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.totalWins = totalWins;
        this.totalLoses = totalLoses;
        this.totalGames = totalGames;
        this.styleId = 1;
    }

    public User(String username, String passwordHash) {
        this.id = null;
        this.username = username;
        this.passwordHash = passwordHash;
        this.totalWins = 0;
        this.totalLoses = 0;
        this.totalGames = 0;
        this.styleId = 1; // Дефолтный стиль
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLoses() {
        return totalLoses;
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getStyleId() {
        return styleId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setStyleId(int styleId) {
        this.styleId = styleId;
    }

    public void addWin() {
        totalWins += 1;
        totalGames += 1;
    }

    public void addLose() {
        totalLoses += 1;
        totalGames += 1;
    }
}