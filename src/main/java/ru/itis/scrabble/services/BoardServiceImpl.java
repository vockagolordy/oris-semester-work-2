package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;
import ru.itis.scrabble.models.BoardCell;
import ru.itis.scrabble.models.CellType;

import java.util.*;

public class BoardServiceImpl implements BoardService {

    @Override
    public List<List<TilePlacementDTO>> findAllWords(List<TilePlacementDTO> newPlacements, Board board) {
        if (newPlacements.isEmpty()) return Collections.emptyList();

        List<List<TilePlacementDTO>> allWords = new ArrayList<>();

        // 1. Находим главное слово (вдоль которого выложены фишки)
        boolean horizontal = isPlacementsHorizontal(newPlacements);
        List<TilePlacementDTO> mainWord = findFullWordAt(newPlacements.get(0), horizontal, board, newPlacements);
        if (mainWord.size() > 1 || (mainWord.size() == 1 && isFirstMove(board))) {
            allWords.add(mainWord);
        }

        // 2. Находим все перпендикулярные слова (пересечения)
        for (TilePlacementDTO p : newPlacements) {
            List<TilePlacementDTO> crossWord = findFullWordAt(p, !horizontal, board, newPlacements);
            if (crossWord.size() > 1) {
                allWords.add(crossWord);
            }
        }

        return allWords;
    }

    @Override
    public boolean checkGeometry(List<TilePlacementDTO> newPlacements, Board board, boolean isFirstMove) {
        if (newPlacements.isEmpty()) return false;

        // 1. Проверка на одной ли линии (X или Y)
        boolean horizontal = newPlacements.stream().allMatch(p -> p.y() == newPlacements.get(0).y());
        boolean vertical = newPlacements.stream().allMatch(p -> p.x() == newPlacements.get(0).x());
        if (!horizontal && !vertical) return false;

        // 2. Если первый ход — должна быть фишка в центре (7,7)
        if (isFirstMove) {
            boolean hasCenter = newPlacements.stream().anyMatch(p -> p.x() == 7 && p.y() == 7);
            if (!hasCenter) return false;
        }

        // 3. Проверка на "дырки" внутри выставленного ряда
        List<TilePlacementDTO> mainWord = findFullWordAt(newPlacements.get(0), horizontal, board, newPlacements);
        for (TilePlacementDTO p : newPlacements) {
            if (!mainWord.contains(p)) return false; // Все новые фишки должны быть частью одного непрерывного слова
        }

        // 4. Если не первый ход — должна быть связь с уже стоящими фишками
        if (!isFirstMove) {
            boolean connected = false;
            for (TilePlacementDTO p : newPlacements) {
                if (hasAdjacentOldTile(p, board)) {
                    connected = true;
                    break;
                }
            }
            if (!connected) return false;
        }

        return true;
    }

    /**
     * Создает и инициализирует игровое поле 15x15 с классической расстановкой бонусов.
     * @return объект Board с настроенными бонусными клетками.
     */
    @Override
    public Board createInitializedBoard() {
        BoardCell[][] cells = new BoardCell[15][15];

        // 1. Сначала заполняем всё пустыми клетками
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                cells[y][x] = new BoardCell(CellType.NONE);
            }
        }

        // 2. Расставляем Triple Word Score (TWS) - красные клетки
        int[][] twsCoords = {{0,0}, {0,7}, {0,14}, {7,0}, {7,14}, {14,0}, {14,7}, {14,14}};
        setBonus(cells, twsCoords, CellType.TWS);

        // 3. Расставляем Double Word Score (DWS) - розовые клетки
        int[][] dwsCoords = {
                {1,1}, {2,2}, {3,3}, {4,4}, {1,13}, {2,12}, {3,11}, {4,10},
                {13,1}, {12,2}, {11,3}, {10,4}, {13,13}, {12,12}, {11,11}, {10,10}, {7,7}
        };
        setBonus(cells, dwsCoords, CellType.DWS);

        // 4. Расставляем Triple Letter Score (TLS) - синие клетки
        int[][] tlsCoords = {
                {1,5}, {1,9}, {5,1}, {5,5}, {5,9}, {5,13},
                {9,1}, {9,5}, {9,9}, {9,13}, {13,5}, {13,9}
        };
        setBonus(cells, tlsCoords, CellType.TLS);

        // 5. Расставляем Double Letter Score (DLS) - голубые клетки
        int[][] dlsCoords = {
                {0,3}, {0,11}, {2,6}, {2,8}, {3,0}, {3,7}, {3,14}, {6,2}, {6,6}, {6,8}, {6,12},
                {7,3}, {7,11}, {8,2}, {8,6}, {8,8}, {8,12}, {11,0}, {11,7}, {11,14}, {12,6}, {12,8}, {14,3}, {14,11}
        };
        setBonus(cells, dlsCoords, CellType.DLS);

        return new Board(cells);
    }

    /**
     * Вспомогательный метод для массовой установки типа клетки по координатам
     */
    private void setBonus(BoardCell[][] cells, int[][] coords, CellType type) {
        for (int[] coord : coords) {
            int y = coord[0];
            int x = coord[1];
            cells[y][x] = new BoardCell(type);
        }
    }

    // Вспомогательный метод: собирает слово целиком, учитывая новые и старые фишки
    private List<TilePlacementDTO> findFullWordAt(TilePlacementDTO start, boolean horizontal, Board board, List<TilePlacementDTO> newPlacements) {
        List<TilePlacementDTO> word = new ArrayList<>();
        int x = start.x();
        int y = start.y();
        BoardCell[][] cells = board.getBoardCells();

        // Идем в начало слова (влево или вверх)
        while (hasAnyTileAt(x - (horizontal ? 1 : 0), y - (horizontal ? 0 : 1), cells, newPlacements)) {
            x -= (horizontal ? 1 : 0);
            y -= (horizontal ? 0 : 1);
        }

        // Собираем слово целиком (вправо или вниз)
        while (hasAnyTileAt(x, y, cells, newPlacements)) {
            word.add(getTileFromBoardOrNew(x, y, cells, newPlacements));
            x += (horizontal ? 1 : 0);
            y += (horizontal ? 0 : 1);
        }

        return word;
    }

    private boolean hasAnyTileAt(int x, int y, BoardCell[][] cells, List<TilePlacementDTO> newPlacements) {
        if (x < 0 || x >= 15 || y < 0 || y >= 15) return false;
        if (cells[y][x].getTile() != null) return true;
        return newPlacements.stream().anyMatch(p -> p.x() == x && p.y() == y);
    }

    private TilePlacementDTO getTileFromBoardOrNew(int x, int y, BoardCell[][] cells, List<TilePlacementDTO> newPlacements) {
        // Сначала ищем в новых
        for (TilePlacementDTO p : newPlacements) {
            if (p.x() == x && p.y() == y) return p;
        }
        // Если нет в новых — берем с доски
        return new TilePlacementDTO(cells[y][x].getTile(), x, y);
    }

    private boolean isPlacementsHorizontal(List<TilePlacementDTO> placements) {
        if (placements.size() < 2) return true; // Одиночная фишка может считаться началом горизонтального
        return placements.get(0).y() == placements.get(1).y();
    }

    private boolean hasAdjacentOldTile(TilePlacementDTO p, Board board) {
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        BoardCell[][] cells = board.getBoardCells();
        for (int i = 0; i < 4; i++) {
            int nx = p.x() + dx[i];
            int ny = p.y() + dy[i];
            if (nx >= 0 && nx < 15 && ny >= 0 && ny < 15 && cells[ny][nx].getTile() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean isFirstMove(Board board) {
        return board.isEmpty();
    }
}