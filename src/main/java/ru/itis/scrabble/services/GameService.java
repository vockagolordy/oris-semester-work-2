package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;

import java.util.List;

public interface GameService {

    void startGame();

    void endGame();

    boolean isValidTilePosition(TilePlacementDTO tilePlacementDTO);

    boolean skipTurn();

    boolean makeMove(List<TilePlacementDTO> tiles);

    boolean isBagEmpty();

    Board getBoard();

    boolean isGameFinished();
}
