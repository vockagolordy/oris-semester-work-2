package ru.itis.scrabble.models;

/**
 * Модель клетки игрового поля.
 * <p>
 * Представляет одну клетку на доске, которая может содержать фишку
 * и иметь специальный тип (например, бонусную клетку).
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code cellType} - тип клетки</li>
 *     <li>{@code tile} - фишка, размещенная на этой клетке, или {@code null} если клетка пуста</li>
 * </ul>
 */
public class BoardCell {
    private final CellType cellType;

    private Tile tile;

    public BoardCell(CellType cellType) {
        this.cellType = cellType;
    }

    public CellType getCellType() {
        return cellType;
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }
}
