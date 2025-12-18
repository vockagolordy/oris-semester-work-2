package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import java.util.List;
import java.util.*;

public class ScoringServiceImpl implements ScoringService {

    private static final int BOARD_SIZE = 15;
    private static final int CENTER = BOARD_SIZE / 2;
    private static final int BINGO_BONUS = 50;

    public ScoringServiceImpl() {
    }

    @Override
    public int countScore(Board board, List<TilePlacementDTO> tilePlacements) {
        if (board == null || tilePlacements == null || tilePlacements.isEmpty()) {
            return 0;
        }

        // Проверяем bingo бонус (все 7 фишек использованы)
        boolean isBingo = tilePlacements.size() == 7;

        // Находим все образованные слова
        List<List<BoardCell>> formedWords = findFormedWords(board, tilePlacements);

        // Считаем очки за все слова
        int totalScore = 0;
        for (List<BoardCell> word : formedWords) {
            totalScore += calculateWordScore(word, tilePlacements);
        }

        // Добавляем bingo бонус
        if (isBingo) {
            totalScore += BINGO_BONUS;
        }

        return totalScore;
    }

    private int calculateWordScore(List<BoardCell> wordCells, List<TilePlacementDTO> newPlacements) {
        int baseScore = 0;
        int wordMultiplier = 1;

        // Создаем Set для быстрой проверки, является ли фишка новой
        Set<String> newTilePositions = new HashSet<>();
        for (TilePlacementDTO placement : newPlacements) {
            newTilePositions.add(placement.x() + "," + placement.y());
        }

        for (BoardCell cell : wordCells) {
            Tile tile = cell.getTile();
            if (tile == null) continue;

            int tileScore = tile.getPoints();
            CellType cellType = cell.getCellType();

            // Проверяем, является ли эта фишка новой (поставлена в этом ходе)
            // Если да - применяем бонусы клетки
            boolean isNewTile = newTilePositions.contains(getCellPosition(cell, wordCells));

            if (isNewTile) {
                // Применяем бонусы букв (только к новым фишкам)
                if (cellType == CellType.DLS) {
                    tileScore *= 2;
                } else if (cellType == CellType.TLS) {
                    tileScore *= 3;
                }

                // Учитываем бонусы слов
                if (cellType == CellType.DWS) {
                    wordMultiplier *= 2;
                } else if (cellType == CellType.TWS) {
                    wordMultiplier *= 3;
                }
            }

            baseScore += tileScore;
        }

        return baseScore * wordMultiplier;
    }

    private List<List<BoardCell>> findFormedWords(Board board, List<TilePlacementDTO> newPlacements) {
        List<List<BoardCell>> formedWords = new ArrayList<>();

        if (board == null || newPlacements.isEmpty()) {
            return formedWords;
        }

        BoardCell[][] cells = board.getBoardCells();

        // Для каждой новой фишки проверяем слова по горизонтали и вертикали
        for (TilePlacementDTO placement : newPlacements) {
            int x = placement.x();
            int y = placement.y();

            if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
                continue;
            }

            // Проверяем горизонтальное слово
            List<BoardCell> horizontalWord = getWordInDirection(cells, x, y, true);
            if (horizontalWord.size() > 1) {
                formedWords.add(horizontalWord);
            }

            // Проверяем вертикальное слово
            List<BoardCell> verticalWord = getWordInDirection(cells, x, y, false);
            if (verticalWord.size() > 1) {
                formedWords.add(verticalWord);
            }
        }

        // Удаляем дубликаты слов
        return removeDuplicateWords(formedWords);
    }

    private List<BoardCell> getWordInDirection(BoardCell[][] cells, int startX, int startY, boolean horizontal) {
        List<BoardCell> word = new ArrayList<>();

        // Находим начало слова (двигаемся назад, пока есть фишки)
        int currentX = startX;
        int currentY = startY;

        if (horizontal) {
            while (currentX > 0 && cells[startY][currentX - 1].getTile() != null) {
                currentX--;
            }
        } else {
            while (currentY > 0 && cells[currentY - 1][startX].getTile() != null) {
                currentY--;
            }
        }

        // Собираем слово (двигаемся вперед, пока есть фишки)
        while (currentX < BOARD_SIZE && currentY < BOARD_SIZE) {
            BoardCell cell = cells[currentY][currentX];
            if (cell.getTile() == null) {
                break;
            }
            word.add(cell);

            if (horizontal) {
                currentX++;
            } else {
                currentY++;
            }
        }

        return word;
    }

    private List<List<BoardCell>> removeDuplicateWords(List<List<BoardCell>> words) {
        List<List<BoardCell>> uniqueWords = new ArrayList<>();
        Set<String> wordHashes = new HashSet<>();

        for (List<BoardCell> word : words) {
            String hash = getWordHash(word);
            if (!wordHashes.contains(hash)) {
                wordHashes.add(hash);
                uniqueWords.add(word);
            }
        }

        return uniqueWords;
    }

    private String getWordHash(List<BoardCell> word) {
        StringBuilder sb = new StringBuilder();
        for (BoardCell cell : word) {
            sb.append(cell.getTile().getLetter());
        }
        return sb.toString();
    }

    private String getCellPosition(BoardCell cell, List<BoardCell> word) {
        // Находим позицию клетки в массиве доски
        // Это упрощенная версия - в реальности нужно получить координаты из доски
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                // Здесь нужно сравнить объекты или найти по координатам
                // Для примера используем простой подход
            }
        }
        return ""; // В реальной реализации нужно вернуть координаты
    }

    // Вспомогательные публичные методы

    public int calculateTilePenalty(List<Tile> tiles) {
        int penalty = 0;
        for (Tile tile : tiles) {
            penalty += tile.getPoints();
        }
        return penalty;
    }

    public boolean isBingoMove(List<TilePlacementDTO> placements) {
        return placements.size() == 7;
    }
}