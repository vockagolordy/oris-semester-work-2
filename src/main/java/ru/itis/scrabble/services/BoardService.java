package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;
import ru.itis.scrabble.models.Tile;

public interface BoardService {

    void createBoard();

    boolean isValidTilePosition(TilePlacementDTO tilePlacement);

    void placeTile(TilePlacementDTO placement);

    Tile getTileAt(int x, int y);

    Board getBoard();
}
