package ru.itis.scrabble.models;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Модель мешка с фишками.
 * <p>
 * Содержит все доступные для игры фишки и отвечает за их выдачу.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code tiles} - список всех фишек, оставшихся в мешке</li>
 * </ul>
 */
public class Bag {

    private final List<Tile> tiles;

    public Bag(List<Tile> tiles) {
        this.tiles = tiles;
    }

    public Tile takeTiles() {

        if (tiles.isEmpty())
            return null;

        return tiles.remove((new Random()).nextInt(tiles.size()));
    }

    public boolean isEmpty() {
        return tiles.isEmpty();
    }
}
