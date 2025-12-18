package ru.itis.scrabble.models;

/**
 * Модель игрового поля.
 * <p>
 * Представляет собой двумерную сетку клеток, на которые размещаются фишки.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code cells} - двумерный массив клеток игрового поля</li>
 * </ul>
 */
public class Board {
    private final BoardCell[][] boardCells;

    public Board(BoardCell[][] boardCells) {
        this.boardCells = boardCells;
    }

    public BoardCell[][] getBoardCells() {
        return boardCells;
    }

    public void setCell(int x, int y, Tile tile) {
        if (boardCells[y][x].getTile() != null)
            return;

        boardCells[y][x].setTile(tile);
    }
}
