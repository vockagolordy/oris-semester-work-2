package ru.itis.scrabble.models;

import java.util.List;

public class GameSession {
    private final Board board;
    private final Bag bag;
    private final List<Player> players;
    private int currentPlayerIndex;
    private int localPlayerIndex;
    private boolean isGameOver;

    public GameSession(Board board, Bag bag, List<Player> players) {
        this.board = board;
        this.bag = bag;
        this.players = players;
        this.currentPlayerIndex = 0;
        this.isGameOver = false;
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Player getLocalPlayer() {
        return players.get(localPlayerIndex);
    }

    // Геттеры
    public Board getBoard() { return board; }
    public Bag getBag() { return bag; }
    public List<Player> getPlayers() { return players; }
    public boolean isGameOver() { return isGameOver; }
    public void setGameOver(boolean gameOver) { isGameOver = gameOver; }
}