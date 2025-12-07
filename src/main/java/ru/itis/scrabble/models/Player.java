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

    private int score;

    private final List<Tile> availableTiles;

    public Player(Long userId, int score, List<Tile> availableTiles) {
        this.userId = userId;
        this.score = score;
        this.availableTiles = availableTiles;
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

    public List<Tile> getAvailableLetters() {
        return availableTiles;
    }

    public void addAvailableLetters(List<Tile> tiles) {
        availableTiles.addAll(tiles);
    }

    public void removeAvailableLetters(List<Tile> tiles) {
        availableTiles.removeAll(tiles);
    }
}
