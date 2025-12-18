package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Tile;

import java.util.List;

public interface BagService {
    void fullBag();

    List<Tile> takeTiles(int amount);

    boolean isEmpty();

    int getRemainingCount();
}
