package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;

import java.util.List;

public interface WordService {
    boolean checkWords(List<List<TilePlacementDTO>> allWords);
}
