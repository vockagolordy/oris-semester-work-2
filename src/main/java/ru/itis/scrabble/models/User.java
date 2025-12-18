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
 * </ul>
 */
public class User {
    private Long id;

    private String username;

    private String passwordHash;

    /**
     * <b>Нет сеттера.</b>
     * Увеличивается методом {@link #addWin()} после завершения игры.
     */
    private int totalWins;

    /**
     * <b>Нет сеттера.</b>
     * Увеличивается методом {@link #addLose()} после завершения игры.
     */
    private int totalLoses;

    /**
     * <b>Нет сеттера.</b>
     * Увеличивается вместе с методами {@link #addWin()}, {@link #addLose()}.
     */
    private int totalGames;

    private int styleId;

    public User(Long id, String username, String passwordHash, int totalWins, int totalLoses, int totalGames) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.totalWins = totalWins;
        this.totalLoses = totalLoses;
        this.totalGames = totalGames;
    }

    public User(String username, String passwordHash) {
        this.id = null;
        this.username = username;
        this.passwordHash = passwordHash;
        this.totalWins = 0;
        this.totalLoses = 0;
        this.totalGames = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void addWin() {
        totalWins += 1;
        totalGames += 1;
    }

    public int getTotalLoses() {
        return totalLoses;
    }

    public void addLose() {
        totalLoses += 1;
        totalGames += 1;
    }

    public int getTotalGames() {
        return totalGames;
    }
}
