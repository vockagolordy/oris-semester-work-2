package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Bag;
import ru.itis.scrabble.models.Tile;

import java.util.List;

public interface BagService {
    Bag fullBag();

    List<Tile> takeTiles(Bag bag, int amount);

    boolean isEmpty(Bag bag);

    int getRemainingCount(Bag bag);
}
