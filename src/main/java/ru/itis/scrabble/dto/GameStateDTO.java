package ru.itis.scrabble.dto;

import ru.itis.scrabble.models.Board;
import ru.itis.scrabble.models.Player;
import java.util.List;

public class GameStateDTO {
    private Board board;
    private List<Player> players;
    private int currentPlayerIndex;
    private int bagCount;
    private boolean isGameOver;

    public GameStateDTO(Board board, List<Player> players, int currentPlayerIndex,
                       int bagCount, boolean isGameOver) {
        this.board = board;
        this.players = players;
        this.currentPlayerIndex = currentPlayerIndex;
        this.bagCount = bagCount;
        this.isGameOver = isGameOver;
    }

    // Getters
    public Board getBoard() { return board; }
    public List<Player> getPlayers() { return players; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public int getBagCount() { return bagCount; }
    public boolean isGameOver() { return isGameOver; }
}