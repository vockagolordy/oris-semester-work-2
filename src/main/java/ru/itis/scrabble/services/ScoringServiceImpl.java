package ru.itis.scrabble.services.impl;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;
import ru.itis.scrabble.models.BoardCell;
import ru.itis.scrabble.models.CellType;
import ru.itis.scrabble.models.Tile;
import ru.itis.scrabble.services.ScoringService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ScoringServiceImpl implements ScoringService {

    @Override
    public int countScore(List<TilePlacementDTO> newPlacements, List<List<TilePlacementDTO>> allWords, Board board) {
        int totalTurnScore = 0;

        // Создаем сет координат новых фишек для быстрой проверки бонусов
        Set<String> newPositions = newPlacements.stream()
                .map(p -> p.x() + "," + p.y())
                .collect(Collectors.toSet());

        for (List<TilePlacementDTO> word : allWords) {
            totalTurnScore += calculateWordScore(word, newPositions, board);
        }

        return totalTurnScore;
    }

    private int calculateWordScore(List<TilePlacementDTO> word, Set<String> newPositions, Board board) {
        int wordMultiplier = 1;
        int wordPoints = 0;

        for (TilePlacementDTO placement : word) {
            int x = placement.x();
            int y = placement.y();
            Tile tile = placement.tile();
            BoardCell cell = board.getBoardCells()[y][x];

            int letterPoints = tile.getPoints();

            // Проверяем, является ли фишка новой в этом ходу
            if (newPositions.contains(x + "," + y)) {
                CellType type = cell.getCellType();

                switch (type) {
                    case DLS -> letterPoints *= 2;
                    case TLS -> letterPoints *= 3;
                    case DWS -> wordMultiplier *= 2;
                    case TWS -> wordMultiplier *= 3;
                    case NONE -> {}
                }
            }

            wordPoints += letterPoints;
        }

        return wordPoints * wordMultiplier;
    }
}