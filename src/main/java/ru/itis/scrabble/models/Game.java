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

    private final Board board;

    private final Bag bag;

    private Long activePlayerId;

    public Game(List<Player> players, Board board, Bag bag, Long activePlayerId) {
        this.players = players;
        this.board = board;
        this.bag = bag;
        this.activePlayerId = activePlayerId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Board getBoard() {
        return board;
    }

    public Bag getBag() {
        return bag;
    }

    public Long getActivePlayerId() {
        return activePlayerId;
    }

    public void setActivePlayerId(Long activePlayerId) {
        this.activePlayerId = activePlayerId;
    }
}
