package ru.itis.scrabble.models;

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

    private boolean isConnected;

    private int score;

    private final List<Tile> rack;

    public Player(Long userId, int score, List<Tile> rack) {
        this.userId = userId;
        this.score = score;
        this.rack = rack;
    }

    public Long getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public void increaseScore(int points) {
        score += points;
    }

    public boolean isConnected() {
        return isConnected;
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
