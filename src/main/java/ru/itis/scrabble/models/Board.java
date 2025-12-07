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
    private final BoardCell[][] cells;

    public Board(BoardCell[][] cells) {
        this.cells = cells;
    }

    public BoardCell[][] getCells() {
        return cells;
    }

    public void setCell(int x, int y, Tile tile) {
        if (cells[y][x].getTile() != null)
            return;

        cells[y][x].setTile(tile);
    }
}
