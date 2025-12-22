package ru.itis.scrabble.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessage;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.network.ClientSession;
import ru.itis.scrabble.network.MessageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionServiceImpl implements GameSessionService {

    private final BoardService boardService;
    private final WordService wordService;
    private final ScoringService scoringService;
    private final BagService bagService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Храним активные сессии. Ключ — ID комнаты (порт), значение — состояние игры
    private final Map<Integer, GameSession> games = new ConcurrentHashMap<>();

    // Храним список активных сетевых сессий для каждой комнаты для рассылки
    private final Map<Integer, List<ClientSession>> roomSessions = new ConcurrentHashMap<>();

    public GameSessionServiceImpl(BoardService boardService, WordService wordService,
                                  ScoringService scoringService, BagService bagService) {
        this.boardService = boardService;
        this.wordService = wordService;
        this.scoringService = scoringService;
        this.bagService = bagService;
    }

    @Override
    public void startNewGame(int roomId, List<Player> players, List<ClientSession> sessions) {
        // 1. Инициализируем доску с бонусами через BoardService
        Board board = boardService.createInitializedBoard();

        // 2. Наполняем мешок
        bagService.fullBag();

        // 3. Раздаем фишки игрокам
        for (Player player : players) {
            player.getRack().clear();
            player.addTiles(bagService.takeTiles(7));
        }

        // 4. Создаем игру и сохраняем сессии для рассылки
        GameSession game = new GameSession(board, null, players);
        games.put(roomId, game);
        roomSessions.put(roomId, new ArrayList<>(sessions));

        broadcastGameState(roomId);
    }

    @Override
    public boolean makeMove(int roomId, Long userId, List<TilePlacementDTO> placements) {
        GameSession session = games.get(roomId);
        if (session == null || session.isGameOver()) return false;

        Player currentPlayer = session.getCurrentPlayer();

        // 1. Проверка очереди хода
        if (!currentPlayer.getUserId().equals(userId)) {
            sendErrorMessage(roomId, userId, "Сейчас не ваш ход!");
            return false;
        }

        // 2. Геометрия (Уровень 1)
        boolean isFirstMove = session.getBoard().isEmpty();
        if (!boardService.checkGeometry(placements, session.getBoard(), isFirstMove)) {
            sendErrorMessage(roomId, userId, "Некорректное расположение фишек!");
            return false;
        }

        // 3. Поиск слов и Словарь (Уровень 2)
        List<List<TilePlacementDTO>> allWords = boardService.findAllWords(placements, session.getBoard());
        if (!wordService.checkWords(allWords)) {
            sendErrorMessage(roomId, userId, "Слова нет в словаре!");
            return false;
        }

        // 4. Подсчет очков
        int turnScore = scoringService.countScore(placements, allWords, session.getBoard());
        currentPlayer.increaseScore(turnScore);

        // 5. Фиксация на доске
        for (TilePlacementDTO p : placements) {
            session.getBoard().setCell(p.x(), p.y(), p.tile());
        }

        // 6. Обновление руки
        List<Tile> usedTiles = placements.stream().map(TilePlacementDTO::tile).toList();
        currentPlayer.removeTiles(usedTiles);
        currentPlayer.addTiles(bagService.takeTiles(placements.size()));

        // 7. Переход хода
        session.nextTurn();

        // 8. Рассылка обновленного состояния всем участникам
        broadcastGameState(roomId);

        return true;
    }

    private void broadcastGameState(int roomId) {
        GameSession game = games.get(roomId);
        List<ClientSession> sessions = roomSessions.get(roomId);

        if (game == null || sessions == null) return;

        try {
            // Сериализуем состояние игры в JSON
            String gameStateJson = objectMapper.writeValueAsString(game);
            NetworkMessage syncMsg = new NetworkMessage(MessageType.SYNC_STATE, gameStateJson, "SERVER");

            for (ClientSession session : sessions) {
                sendMessage(session, syncMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendErrorMessage(int roomId, Long userId, String errorText) {
        List<ClientSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return;

        sessions.stream()
                .filter(s -> userId.equals(s.getUserId()))
                .findFirst()
                .ifPresent(s -> {
                    try {
                        NetworkMessage msg = new NetworkMessage(MessageType.ERROR, errorText, "SERVER");
                        sendMessage(s, msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void sendMessage(ClientSession session, NetworkMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        byte[] body = json.getBytes();

        java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(4);
        header.putInt(body.length);
        header.flip();

        session.getChannel().write(header);
        session.getChannel().write(java.nio.ByteBuffer.wrap(body));
    }
}