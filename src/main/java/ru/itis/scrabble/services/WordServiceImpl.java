package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.util.DictUtil;

import java.util.List;
import java.util.stream.Collectors;

public class WordServiceImpl implements WordService {

    @Override
    public boolean checkWords(List<List<TilePlacementDTO>> allWords) {
        if (allWords == null || allWords.isEmpty()) {
            return false;
        }

        List<String> wordsAsString = allWords.stream()
                .map(this::convertToString)
                .collect(Collectors.toList());

        return DictUtil.areAllWordsValid(wordsAsString);
    }

    private String convertToString(List<TilePlacementDTO> word) {
        return word.stream()
                .map(p -> String.valueOf(p.tile().getLetter()))
                .collect(Collectors.joining());
    }
}