package ru.itis.scrabble.models;

import java.util.List;

/**
 * Модель игровой сессии.
 * <p>
 * Содержит все компоненты игры: игроков, поле, мешок с фишками
 * и отслеживает текущего активного игрока.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code players} - список всех игроков в текущей игре</li>
 *     <li>{@code board} - игровое поле</li>
 *     <li>{@code bag} - мешок с фишками</li>
 *     <li>{@code activePlayerId} - ID игрока, чей сейчас ход</li>
 * </ul>
 */
public class Game {
    private final  List<Player> players;

    private int activePlayerIdx;

    public Game(List<Player> players, int activePlayerIdx) {
        this.players = players;
        this.activePlayerIdx = activePlayerIdx;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getActivePlayerIdx() {
        return activePlayerIdx;
    }

    public void setActivePlayerIdx(int activePlayerIdx) {
        this.activePlayerIdx = activePlayerIdx;
    }
}
