package ru.itis.scrabble.models;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private int totalWins;
    private int totalLoses;
    private int totalGames;
    private int styleId;

    // Пустой конструктор обязателен для JPA
    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.totalWins = 0;
        this.totalLoses = 0;
        this.totalGames = 0;
        this.styleId = 1; // Дефолтный стиль
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getTotalWins() { return totalWins; }
    public int getTotalLoses() { return totalLoses; }
    public int getTotalGames() { return totalGames; }

    public int getStyleId() { return styleId; }
    public void setStyleId(int styleId) { this.styleId = styleId; }

    // Логика обновления статистики
    public void addWin() {
        this.totalWins++;
        addGame();
    }

    public void addLose() {
        this.totalLoses++;
        addGame();
    }

    public void addGame() {
        totalGames++;
    }
}