package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Player;
import ru.itis.scrabble.network.ClientSession;
import ru.itis.scrabble.dto.TilePlacementDTO;
import java.util.List;

public interface GameSessionService {

    void startNewGame(int roomId, List<Player> players, List<ClientSession> sessions);

    boolean makeMove(int roomId, Long userId, List<TilePlacementDTO> placements);
}