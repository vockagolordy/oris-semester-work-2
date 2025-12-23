package ru.itis.scrabble.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель игрока в конкретной игре.
 * <p>
 * Представляет участника игры с его текущим состоянием: очками и доступными фишками.
 * Связан с моделью {@code User} через {@code userId}.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code userId} - ID пользователя</li>
 *     <li>{@code score} - количество очков в игре</li>
 *     <li>{@code availableTiles} - фишки, доступные игроку</li>
 * </ul>
 */
public class Player {
    private final Long userId;

    private final String username;

    private boolean isConnected;

    private int score;

    private int lastPoints;

    private final List<Tile> rack;

    public Player(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.score = 0;
        this.lastPoints = 0;
        this.rack = new ArrayList<>();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public void increaseScore(int points) {
        lastPoints = points;
        score += points;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int getLastPoints() {
        return lastPoints;
    }

    public List<Tile> getRack() {
        return rack;
    }

    public void addTiles(List<Tile> tiles) {
        rack.addAll(tiles);
    }

    public void removeTiles(List<Tile> tiles) {
        rack.removeAll(tiles);
    }
}
