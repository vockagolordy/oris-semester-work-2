package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;
import java.util.List;

public interface BoardService {
    List<List<TilePlacementDTO>> findAllWords(List<TilePlacementDTO> newPlacements, Board board);

    boolean checkGeometry(List<TilePlacementDTO> newPlacements, Board board, boolean isFirstMove);

    Board createInitializedBoard();
}