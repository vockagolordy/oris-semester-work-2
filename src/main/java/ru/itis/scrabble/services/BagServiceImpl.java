package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Tile;
import ru.itis.scrabble.models.Bag;

import java.util.*;

public class BagServiceImpl implements BagService {
    private Bag bag;

    public BagServiceImpl() {
        this.bag = null;
        fullBag();
    }

    @Override
    public void fullBag() {
        List<Tile> tiles = new ArrayList<>();

        // Стандартное распределение фишек для английского Scrabble
        // A-9, B-2, C-2, D-4, E-12, F-2, G-3, H-2, I-9, J-1, K-1, L-4, M-2,
        // N-6, O-8, P-2, Q-1, R-6, S-4, T-6, U-4, V-2, W-2, X-1, Y-2, Z-1

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

        this.bag = new Bag(tiles);
    }

    private void addTiles(List<Tile> tiles, Tile tile, int count) {
        for (int i = 0; i < count; i++) {
            tiles.add(tile);
        }
    }

    @Override
    public List<Tile> takeTiles(int amount) {
        if (bag == null || bag.isEmpty()) {
            return Collections.emptyList();
        }

        List<Tile> takenTiles = new ArrayList<>();
        for (int i = 0; i < amount && !bag.isEmpty(); i++) {
            Tile tile = bag.takeTiles();
            if (tile != null) {
                takenTiles.add(tile);
            }
        }

        return takenTiles;
    }

    @Override
    public boolean isEmpty() {
        return bag == null || bag.isEmpty();
    }

    @Override
    public int getRemainingCount() {
        return bag.getRemainingCount();
    }
}