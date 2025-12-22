package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.Board;

import java.util.List;

public interface ScoringService {

    int countScore(List<TilePlacementDTO> newPlacements, List<List<TilePlacementDTO>> allWords, Board board);
}
