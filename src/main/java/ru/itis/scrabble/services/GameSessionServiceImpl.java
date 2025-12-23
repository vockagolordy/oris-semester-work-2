package ru.itis.scrabble.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessage;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.network.ClientSession;
import ru.itis.scrabble.network.MessageType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionServiceImpl implements GameSessionService {

    private final BoardService boardService;
    private final WordService wordService;
    private final ScoringService scoringService;
    private final BagService bagService;
    private final UserService userService; // Добавлено для работы с БД пользователей
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Integer, GameSession> games = new ConcurrentHashMap<>();
    private final Map<Integer, List<ClientSession>> roomSessions = new ConcurrentHashMap<>();

    public GameSessionServiceImpl(BoardService boardService, WordService wordService,
                                  ScoringService scoringService, BagService bagService,
                                  UserService userService) {
        this.boardService = boardService;
        this.wordService = wordService;
        this.scoringService = scoringService;
        this.bagService = bagService;
        this.userService = userService;
    }

    @Override
    public void startNewGame(int roomId, List<Player> players, List<ClientSession> sessions) {
        Board board = boardService.createInitializedBoard();
        Bag bag = bagService.fullBag();

        for (Player player : players) {
            player.getRack().clear();
            player.addTiles(bagService.takeTiles(bag, 7));
        }

        GameSession game = new GameSession(board, bag, players);
        games.put(roomId, game);
        roomSessions.put(roomId, new ArrayList<>(sessions));

        broadcastGameState(roomId);
    }

    @Override
    public boolean makeMove(int roomId, Long userId, List<TilePlacementDTO> placements) {
        GameSession session = games.get(roomId);
        if (session == null || session.isGameOver()) return false;

        Player currentPlayer = session.getCurrentPlayer();

        if (!currentPlayer.getUserId().equals(userId)) {
            sendErrorMessage(roomId, userId, "Сейчас не ваш ход!");
            return false;
        }

        boolean isFirstMove = session.getBoard().isEmpty();
        if (!boardService.checkGeometry(placements, session.getBoard(), isFirstMove)) {
            sendErrorMessage(roomId, userId, "Некорректное расположение фишек!");
            return false;
        }

        List<List<TilePlacementDTO>> allWords = boardService.findAllWords(placements, session.getBoard());
        if (!wordService.checkWords(allWords)) {
            sendErrorMessage(roomId, userId, "Слова нет в словаре!");
            return false;
        }

        int turnScore = scoringService.countScore(placements, allWords, session.getBoard());
        currentPlayer.increaseScore(turnScore);

        for (TilePlacementDTO p : placements) {
            session.getBoard().setCell(p.x(), p.y(), p.tile());
        }

        List<Tile> usedTiles = placements.stream().map(TilePlacementDTO::tile).toList();
        currentPlayer.removeTiles(usedTiles);
        currentPlayer.addTiles(bagService.takeTiles(session.getBag(), placements.size()));

        // Проверка завершения игры
        if (session.getBag().isEmpty() && currentPlayer.getRack().isEmpty()) {
            handleGameOver(roomId, session);
        } else {
            session.nextTurn();
            broadcastGameState(roomId);
        }

        return true;
    }

    private void handleGameOver(int roomId, GameSession session) {
        session.setGameOver(true);
        broadcastGameState(roomId);

        // Сохранение результатов в БД через UserService
        for (Player player : session.getPlayers()) {
            // Предполагаем, что в UserService есть метод обновления статистики
            userService.updateUserStats(player.getUserId(), player.getScore());
        }
    }

    private void broadcastGameState(int roomId) {
        GameSession game = games.get(roomId);
        List<ClientSession> sessions = roomSessions.get(roomId);

        if (game == null || sessions == null) return;

        try {
            String gameStateJson = objectMapper.writeValueAsString(game);
            NetworkMessage syncMsg = new NetworkMessage(MessageType.SYNC_STATE, gameStateJson, "SERVER");

            for (ClientSession session : sessions) {
                sendMessage(session, syncMsg);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при рассылке состояния игры: " + e.getMessage());
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

    /**
     * Исправлено: Централизованный метод отправки.
     * Теперь строго соблюдает протокол [4 байта длины] + [JSON]
     */
    private void sendMessage(ClientSession session, NetworkMessage message) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(message);

        // Выделяем один буфер под всё сообщение (Header + Body)
        ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
        buffer.putInt(body.length); // 4 байта длины
        buffer.put(body);           // Тело JSON
        buffer.flip();

        // Записываем всё в канал сессии
        while (buffer.hasRemaining()) {
            session.getChannel().write(buffer);
        }
    }
}