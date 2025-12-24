package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Player;
import ru.itis.scrabble.network.ClientSession;
import ru.itis.scrabble.dto.TilePlacementDTO;
import java.util.List;

public interface GameSessionService {

    void startNewGame(int roomId, List<Player> players, List<ClientSession> sessions);

    boolean makeMove(int roomId, Long userId, List<TilePlacementDTO> placements);

    // Methods invoked by PacketHandler
    void authenticate(ru.itis.scrabble.network.ClientSession session, String username, String password);

    void commitTurn(ru.itis.scrabble.network.ClientSession session, List<TilePlacementDTO> placements);

    void processPreview(ru.itis.scrabble.network.ClientSession session, List<TilePlacementDTO> placements);

    void handleHeartbeat(ru.itis.scrabble.network.ClientSession session);
}