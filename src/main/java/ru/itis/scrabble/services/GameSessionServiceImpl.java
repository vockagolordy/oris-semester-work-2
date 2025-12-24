package ru.itis.scrabble.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessageDTO;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.network.ClientSession;
import ru.itis.scrabble.network.MessageType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final Map<Integer, java.util.concurrent.ExecutorService> roomExecutors = new ConcurrentHashMap<>();

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
    public void authenticate(ClientSession session, String username, String password) {
        try {
            // Try to login the user via UserService (implementation may hash/verify password)
            ru.itis.scrabble.models.User user = userService.login(username, password);
            if (user != null) {
                session.setUserId(user.getId());
                session.setUsername(user.getUsername());
                String payload = objectMapper.writeValueAsString(Map.of("userId", user.getId(), "username", user.getUsername()));
                NetworkMessageDTO msg = new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.AUTH, "AUTH_SUCCESS|" + payload, "SERVER");
                session.sendMessage(msg);
            } else {
                NetworkMessageDTO err = new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.ERROR, "AUTH_ERROR|Invalid credentials", "SERVER");
                session.sendMessage(err);
            }
        } catch (Exception e) {
            NetworkMessageDTO err = new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.ERROR, "AUTH_ERROR|" + e.getMessage(), "SERVER");
            session.sendMessage(err);
        }
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

        // Create a single-thread executor for this room to serialize game state changes
        java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("game-room-" + roomId + "-executor");
            t.setDaemon(true);
            return t;
        });
        roomExecutors.put(roomId, exec);

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
        List<Player> players = session.getPlayers();
        players.sort((o1, o2) -> o1.getScore() - o2.getScore());
        userService.updateGames(players.getFirst().getUserId(), -1);
        for (Player player: players) {
            userService.updateGames(player.getUserId(), 0);
        }
        userService.updateGames(players.getLast().getUserId(), 1);

        // Shutdown room executor as game finished
        java.util.concurrent.ExecutorService exec = roomExecutors.remove(roomId);
        if (exec != null) {
            exec.shutdownNow();
        }
    }

    private void broadcastGameState(int roomId) {
        GameSession game = games.get(roomId);
        List<ClientSession> sessions = roomSessions.get(roomId);

        if (game == null || sessions == null) return;

        try {
            String gameStateJson = objectMapper.writeValueAsString(game);
            NetworkMessageDTO syncMsg = new NetworkMessageDTO(MessageType.SYNC_STATE, gameStateJson, "SERVER");

            for (ClientSession session : sessions) {
                session.sendMessage(syncMsg);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при рассылке состояния игры: " + e.getMessage());
        }
    }

    @Override
    public void commitTurn(ClientSession session, List<TilePlacementDTO> placements) {
        int roomId = session.getRoomId();
        Long userId = session.getUserId();

        java.util.concurrent.ExecutorService exec = roomExecutors.get(roomId);
        if (exec == null) {
            // No executor available, fallback to immediate execution
            boolean ok = makeMove(roomId, userId, placements);
            try {
                if (ok) session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.GAME_EVENT, "MOVE_ACCEPTED|{}", "SERVER"));
                else session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.ERROR, "MOVE_REJECTED|Move invalid", "SERVER"));
            } catch (Exception e) {
                System.err.println("Error sending commit response: " + e.getMessage());
            }
            return;
        }

        // Submit the move to the room executor to serialize modifications
        exec.submit(() -> {
            boolean ok = makeMove(roomId, userId, placements);
            try {
                if (ok) {
                    session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.GAME_EVENT, "MOVE_ACCEPTED|{}", "SERVER"));
                } else {
                    session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.ERROR, "MOVE_REJECTED|Move invalid", "SERVER"));
                }
            } catch (Exception e) {
                System.err.println("Error sending commit response: " + e.getMessage());
            }
        });
    }

    @Override
    public void processPreview(ClientSession session, List<TilePlacementDTO> placements) {
        int roomId = session.getRoomId();
        GameSession gs = games.get(roomId);
        if (gs == null) return;

        try {
            boolean isFirstMove = gs.getBoard().isEmpty();
            boolean geometryOk = boardService.checkGeometry(placements, gs.getBoard(), isFirstMove);
            if (!geometryOk) {
                session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.GAME_EVENT, "PREVIEW_INVALID|Geometry", "SERVER"));
                return;
            }

            List<List<TilePlacementDTO>> allWords = boardService.findAllWords(placements, gs.getBoard());
            if (!wordService.checkWords(allWords)) {
                session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.GAME_EVENT, "PREVIEW_INVALID|Word not in dictionary", "SERVER"));
                return;
            }

            session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.GAME_EVENT, "PREVIEW_OK|{}", "SERVER"));
        } catch (Exception e) {
            // ignore preview errors quietly
        }
    }

    @Override
    public void handleHeartbeat(ClientSession session) {
        try {
            session.sendMessage(new NetworkMessageDTO(ru.itis.scrabble.network.MessageType.HEARTBEAT, "HEARTBEAT_ACK|{}", "SERVER"));
        } catch (Exception e) {
            // ignore
        }
    }

    private void sendErrorMessage(int roomId, Long userId, String errorText) {
        List<ClientSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return;

        sessions.stream()
                .filter(s -> userId.equals(s.getUserId()))
                .findFirst()
                .ifPresent(s -> {
                    NetworkMessageDTO msg = new NetworkMessageDTO(MessageType.ERROR, errorText, "SERVER");
                    s.sendMessage(msg);
                });
    }

    /**
     * Исправлено: Централизованный метод отправки.
     * Теперь строго соблюдает протокол [4 байта длины] + [JSON]
     */
    // Sending is delegated to ClientSession.sendMessage
}