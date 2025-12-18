package ru.itis.scrabble.DTO;

import ru.itis.scrabble.models.Tile;

public record TilePlacementDTO(
        Tile tile,
        int x,
        int y
) {
}
