package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import java.util.Arrays;

public class BoardServiceImpl implements BoardService {
    private Board board;
    private static final int BOARD_SIZE = 15;
    private static final int CENTER = BOARD_SIZE / 2;

    public BoardServiceImpl() {
        this.board = null;

        createBoard();
    }

    @Override
    public void createBoard() {
        BoardCell[][] cells = new BoardCell[BOARD_SIZE][BOARD_SIZE];

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                cells[i][j] = new BoardCell(CellType.NONE);
            }
        }

        setBonusCells(cells);

        this.board = new Board(cells);
    }

    private void setBonusCells(BoardCell[][] cells) {
        // Центральная клетка - звезда (удвоение слова)
        cells[CENTER][CENTER] = new BoardCell(CellType.DWS);

        // Утроение слова (TWS) - красные клетки
        int[][] twsPositions = {
            {0, 0}, {0, 7}, {0, 14},
            {7, 0}, {7, 14},
            {14, 0}, {14, 7}, {14, 14}
        };
        for (int[] pos : twsPositions) {
            cells[pos[0]][pos[1]] = new BoardCell(CellType.TWS);
        }

        // Удвоение слова (DWS) - розовые клетки
        int[][] dwsPositions = {
            {1, 1}, {1, 13}, {2, 2}, {2, 12}, {3, 3}, {3, 11}, {4, 4}, {4, 10},
            {10, 4}, {10, 10}, {11, 3}, {11, 11}, {12, 2}, {12, 12}, {13, 1}, {13, 13},
            {7, 7}
        };
        for (int[] pos : dwsPositions) {
            cells[pos[0]][pos[1]] = new BoardCell(CellType.DWS);
        }

        // Утроение буквы (TLS) - синие клетки
        int[][] tlsPositions = {
            {1, 5}, {1, 9}, {5, 1}, {5, 5}, {5, 9}, {5, 13},
            {9, 1}, {9, 5}, {9, 9}, {9, 13}, {13, 5}, {13, 9}
        };
        for (int[] pos : tlsPositions) {
            cells[pos[0]][pos[1]] = new BoardCell(CellType.TLS);
        }

        // Удвоение буквы (DLS) - голубые клетки
        int[][] dlsPositions = {
            {0, 3}, {0, 11}, {2, 6}, {2, 8}, {3, 0}, {3, 7}, {3, 14},
            {6, 2}, {6, 6}, {6, 8}, {6, 12}, {7, 3}, {7, 11},
            {8, 2}, {8, 6}, {8, 8}, {8, 12}, {11, 0}, {11, 7}, {11, 14},
            {12, 6}, {12, 8}, {14, 3}, {14, 11}
        };
        for (int[] pos : dlsPositions) {
            cells[pos[0]][pos[1]] = new BoardCell(CellType.DLS);
        }
    }

    @Override
    public boolean isValidTilePosition(TilePlacementDTO tilePlacement) {
        if (board == null) {
            return false;
        }

        int x = tilePlacement.x();
        int y = tilePlacement.y();

        // Проверяем границы
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return false;
        }

        // Проверяем, что клетка пуста
        Tile existingTile = getTileAt(x, y);
        return existingTile == null;
    }

    @Override
    public Tile getTileAt(int x, int y) {
        if (board == null || x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return null;
        }
        return board.getBoardCells()[y][x].getTile();
    }

    @Override
    public void placeTile(TilePlacementDTO placement) {
        if (board != null && isValidTilePosition(placement)) {
            board.setCell(placement.x(), placement.y(), placement.tile());
        }
    }

    @Override
    public Board getBoard() {
        return board;
    }
}