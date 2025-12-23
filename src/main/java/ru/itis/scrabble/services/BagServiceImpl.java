package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Bag;
import ru.itis.scrabble.models.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Реализация сервиса для работы с мешком фишек.
 */
public class BagServiceImpl implements BagService {

    public BagServiceImpl() {
        fullBag();
    }

    @Override
    public Bag fullBag() {
        List<Tile> tiles = new ArrayList<>();

        // Стандартное распределение фишек для английского Scrabble
        addTiles(tiles, Tile.A, 9);
        addTiles(tiles, Tile.B, 2);
        addTiles(tiles, Tile.C, 2);
        addTiles(tiles, Tile.D, 4);
        addTiles(tiles, Tile.E, 12);
        addTiles(tiles, Tile.F, 2);
        addTiles(tiles, Tile.G, 3);
        addTiles(tiles, Tile.H, 2);
        addTiles(tiles, Tile.I, 9);
        addTiles(tiles, Tile.J, 1);
        addTiles(tiles, Tile.K, 1);
        addTiles(tiles, Tile.L, 4);
        addTiles(tiles, Tile.M, 2);
        addTiles(tiles, Tile.N, 6);
        addTiles(tiles, Tile.O, 8);
        addTiles(tiles, Tile.P, 2);
        addTiles(tiles, Tile.Q, 1);
        addTiles(tiles, Tile.R, 6);
        addTiles(tiles, Tile.S, 4);
        addTiles(tiles, Tile.T, 6);
        addTiles(tiles, Tile.U, 4);
        addTiles(tiles, Tile.V, 2);
        addTiles(tiles, Tile.W, 2);
        addTiles(tiles, Tile.X, 1);
        addTiles(tiles, Tile.Y, 2);
        addTiles(tiles, Tile.Z, 1);

        return new Bag(tiles);
    }

    private void addTiles(List<Tile> tiles, Tile tile, int count) {
        for (int i = 0; i < count; i++) {
            tiles.add(tile);
        }
    }

    @Override
    public List<Tile> takeTiles(Bag bag, int amount) {
        if (bag == null || bag.isEmpty()) { //
            return Collections.emptyList();
        }

        List<Tile> takenTiles = new ArrayList<>();
        for (int i = 0; i < amount && !bag.isEmpty(); i++) {
            Tile tile = bag.takeTiles(); // Метод модели Bag использует Random
            if (tile != null) {
                takenTiles.add(tile);
            }
        }
        return takenTiles;
    }

    @Override
    public boolean isEmpty(Bag bag) {
        return bag == null || bag.isEmpty(); //
    }

    @Override
    public int getRemainingCount(Bag bag) {
        return (bag != null) ? bag.getRemainingCount() : 0; //
    }
}