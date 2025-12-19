// NetworkServer.java
package ru.itis.scrabble.network;

import ru.itis.scrabble.network.ConnectionManager;
import ru.itis.scrabble.services.*;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.dto.TilePlacementDTO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkServer {
    private final Selector selector;
    private final Map<Integer, ServerSocketChannel> serverChannels;
    private final Map<SocketChannel, ClientSession> sessions;

    // Сервисы
    private final SecurityService securityService;
    private final RoomService roomService;
    private final UserService userService;
    private final GameService gameService;
    private final BagService bagService;
    private final BoardService boardService;
    private final ScoringService scoringService;

    // Менеджер подключений
    private final ConnectionManager connectionManager;

    private final ScheduledExecutorService scheduler;

    public NetworkServer(
            SecurityService securityService,
            RoomService roomService,
            UserService userService,
            GameService gameService,
            BagService bagService,
            BoardService boardService,
            ScoringService scoringService) {

        this.securityService = securityService;
        this.roomService = roomService;
        this.userService = userService;
        this.gameService = gameService;
        this.bagService = bagService;
        this.boardService = boardService;
        this.scoringService = scoringService;

        this.connectionManager = new ConnectionManager();

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть селектор", e);
        }

        this.serverChannels = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);

        startBackgroundTasks();
        System.out.println("Сервер инициализирован");
    }

    public void startServerOnPort(int port) throws IOException {
        if (serverChannels.containsKey(port)) {
            System.out.println("Сервер уже запущен на порту " + port);
            return;
        }

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        serverChannels.put(port, serverChannel);
        System.out.println("Сервер запущен на порту " + port);
    }

    public void stopServerOnPort(int port) throws IOException {
        ServerSocketChannel serverChannel = serverChannels.remove(port);
        if (serverChannel != null) {
            serverChannel.close();
            System.out.println("Сервер остановлен на порту " + port);
        }
    }

    public void start() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectedKeys.iterator();

                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();

                        if (!key.isValid()) continue;

                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            readFromClient(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Сервер запущен и готов принимать подключения");
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        int port = serverChannel.socket().getLocalPort();
        ClientSession session = new ClientSession(clientChannel, port);
        sessions.put(clientChannel, session);

        System.out.println("Новое подключение на порту " + port + " от " +
                clientChannel.getRemoteAddress());

        sendToClient(clientChannel, "HELLO|" + port);
    }

    private void readFromClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = sessions.get(channel);

        if (session == null) {
            channel.close();
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead;

        try {
            bytesRead = channel.read(buffer);
        } catch (IOException e) {
            handleClientDisconnect(channel);
            return;
        }

        if (bytesRead == -1) {
            handleClientDisconnect(channel);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            String message = new String(buffer.array(), 0, bytesRead).trim();
            processClientMessage(session, message);
            session.updateLastActivity();
        }
    }

    private void processClientMessage(ClientSession session, String rawMessage) {
        String[] messages = rawMessage.split("\n");

        for (String message : messages) {
            if (message.trim().isEmpty()) continue;

            System.out.println("Получено [" + session.getPort() + "]: " + message);

            try {
                String[] parts = message.split("\\|", 2);
                String command = parts[0];
                String data = parts.length > 1 ? parts[1] : "";

                handleCommand(session, command, data);

            } catch (Exception e) {
                e.printStackTrace();
                sendToClient(session.getChannel(), "ERROR|Ошибка обработки команды: " + e.getMessage());
            }
        }
    }

    private void handleCommand(ClientSession session, String command, String data) {
        switch (command) {
            case "REGISTER":
                handleRegister(session, data);
                break;
            case "LOGIN":
                handleLogin(session, data);
                break;
            case "LOGOUT":
                handleLogout(session);
                break;
            case "CREATE_ROOM":
                handleCreateRoom(session, data);
                break;
            case "JOIN":
                handleJoin(session, data);
                break;
            case "LEAVE":
                handleLeave(session);
                break;
            case "START":
                handleStartGame(session);
                break;
            case "MOVE":
                handleMove(session, data);
                break;
            case "SKIP":
                handleSkip(session);
                break;
            case "SURRENDER":
                handleSurrender(session);
                break;
            case "DRAW_OFFER":
                handleDrawOffer(session);
                break;
            case "DRAW_ACCEPT":
                handleDrawAccept(session);
                break;
            case "DRAW_REJECT":
                handleDrawReject(session);
                break;
            case "RECONNECT":
                handleReconnect(session, data);
                break;
            case "LIST_ROOMS":
                handleListRooms(session);
                break;
            case "PING":
                handlePing(session);
                break;
            case "GET_STATE":
                handleGetState(session);
                break;
            case "GET_STYLE":
                handleGetStyle(session);
                break;
            case "SET_STYLE":
                handleSetStyle(session, data);
                break;
            case "GET_AVAILABLE_STYLES":
                handleGetAvailableStyles(session);
                break;
            default:
                sendToClient(session.getChannel(), "ERROR|Неизвестная команда: " + command);
        }
    }

    private void handleRegister(ClientSession session, String data) {
        String[] parts = data.split("\\|");
        if (parts.length != 3) {
            sendToClient(session.getChannel(), "ERROR|Неверный формат: REGISTER|username|password|passwordRepeat");
            return;
        }

        try {
            String username = parts[0];
            String password = parts[1];
            String passwordRepeat = parts[2];

            securityService.signupUser(username, password, passwordRepeat);
            sendToClient(session.getChannel(), "REGISTER_SUCCESS|" + username);

        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка регистрации: " + e.getMessage());
        }
    }

    private void handleLogin(ClientSession session, String data) {
        String[] parts = data.split("\\|");
        if (parts.length != 2) {
            sendToClient(session.getChannel(), "ERROR|Неверный формат: LOGIN|username|password");
            return;
        }

        try {
            String username = parts[0];
            String password = parts[1];

            securityService.loginUser(username, password);
            User user = userService.findByUsername(username);

            if (user == null) {
                sendToClient(session.getChannel(), "ERROR|Пользователь не найден");
                return;
            }

            session.setUserId(user.getId());
            session.setUsername(username);
            session.setAuthenticated(true);

            // Регистрируем подключение в ConnectionManager
            connectionManager.registerConnection(user.getId(), session.getChannel());

            // Используем RoomService для работы с комнатой
            Room room = roomService.getRoomByPort(session.getPort());
            if (room == null) {
                // Создаем новую комнату, если на этом порту еще нет
                room = roomService.createRoom(user.getId(), session.getPort());
                sendToClient(session.getChannel(),
                        "LOGIN_SUCCESS|" + user.getId() + "|" + username +
                                "|HOST|" + session.getPort());
            } else {
                // Присоединяемся к существующей комнате
                if (roomService.joinRoom(user.getId(), session.getPort())) {
                    String role = room.getHostId().equals(user.getId()) ? "HOST" : "GUEST";
                    sendToClient(session.getChannel(),
                            "LOGIN_SUCCESS|" + user.getId() + "|" + username +
                                    "|" + role + "|" + session.getPort());

                    // Уведомляем других игроков
                    notifyRoomPlayers(room, "PLAYER_JOINED|" + user.getId() + "|" + username, user.getId());
                } else {
                    sendToClient(session.getChannel(), "ERROR|Не удалось присоединиться к комнате");
                }
            }

        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка входа: " + e.getMessage());
        }
    }

    private void handleCreateRoom(ClientSession session, String data) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        try {
            int port = Integer.parseInt(data);

            if (serverChannels.containsKey(port)) {
                sendToClient(session.getChannel(), "ERROR|Порт " + port + " уже занят");
                return;
            }

            // Запускаем сервер на новом порту
            startServerOnPort(port);

            // Создаем комнату через RoomService
            Room room = roomService.createRoom(session.getUserId(), port);
            if (room != null) {
                sendToClient(session.getChannel(), "ROOM_CREATED|" + port);
            } else {
                sendToClient(session.getChannel(), "ERROR|Не удалось создать комнату");
            }

        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка создания комнаты: " + e.getMessage());
        }
    }

    private void handleJoin(ClientSession session, String data) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        try {
            int port = Integer.parseInt(data);

            // Используем RoomService для присоединения
            if (roomService.joinRoom(session.getUserId(), port)) {
                Room room = roomService.getRoomByPort(port);
                if (room != null) {
                    sendToClient(session.getChannel(),
                            "JOIN_SUCCESS|" + port + "|" + room.getHostId() +
                                    "|" + room.getUserCount());

                    // Уведомляем всех в комнате
                    notifyRoomPlayers(room,
                            "PLAYER_JOINED|" + session.getUserId() + "|" + session.getUsername(),
                            session.getUserId());

                    // Если комната полная
                    if (room.isFull()) {
                        notifyRoomPlayers(room, "ROOM_FULL", null);
                    }
                }
            } else {
                sendToClient(session.getChannel(), "ERROR|Не удалось присоединиться");
            }

        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка присоединения: " + e.getMessage());
        }
    }

    private void handleStartGame(ClientSession session) {
        Room room = roomService.getRoomByPort(session.getPort());

        if (room == null) {
            sendToClient(session.getChannel(), "ERROR|Комната не найдена");
            return;
        }

        if (!room.getHostId().equals(session.getUserId())) {
            sendToClient(session.getChannel(), "ERROR|Только хост может начать игру");
            return;
        }

        if (!room.isFull()) {
            sendToClient(session.getChannel(), "ERROR|Нужно два игрока для начала игры");
            return;
        }

        if (room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра уже начата");
            return;
        }

        try {
            // Инициализируем сервисы для новой игры
            bagService.fullBag();
            boardService.createBoard();
            gameService.startGame();

            // Получаем пользователей из комнаты
            List<Long> userIds = room.getUsers();
            List<Player> players = new ArrayList<>();

            for (Long userId : userIds) {
                User user = userService.findById(userId);
                if (user != null) {
                    Player player = new Player(userId, user.getUsername());
                    // Раздаем начальные фишки
                    List<Tile> tiles = bagService.takeTiles(7);
                    player.addTiles(tiles);
                    players.add(player);
                }
            }

            // Создаем объект Game
            int firstPlayerIndex = new Random().nextInt(players.size());
            Game game = new Game(players, firstPlayerIndex);
            room.setGame(game);
            room.setGameStarted(true);

            // Используем GameService для сериализации состояния
            String gameState = gameService.serializeGameState(room);
            notifyRoomPlayers(room, "GAME_STARTED|" + gameState, null);

            // Уведомляем, чей ход первый
            Player firstPlayer = game.getPlayers().get(firstPlayerIndex);
            notifyRoomPlayers(room, "TURN_START|" + firstPlayer.getUserId() + "|90", null);

        } catch (Exception e) {
            e.printStackTrace();
            sendToClient(session.getChannel(), "ERROR|Ошибка начала игры: " + e.getMessage());
        }
    }

    private void handleMove(ClientSession session, String data) {
        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        Game game = room.getGame();
        if (game == null) {
            sendToClient(session.getChannel(), "ERROR|Игра не найдена");
            return;
        }

        Player currentPlayer = game.getPlayers().get(game.getActivePlayerIdx());

        if (!currentPlayer.getUserId().equals(session.getUserId())) {
            sendToClient(session.getChannel(), "ERROR|Не ваш ход");
            return;
        }

        try {
            // Парсим ход
            List<TilePlacementDTO> placements = parseTilePlacements(data);

            // Проверяем валидность позиций
            for (TilePlacementDTO placement : placements) {
                if (!boardService.isValidTilePosition(placement)) {
                    sendToClient(session.getChannel(), "ERROR|Неверная позиция фишки");
                    return;
                }
            }

            // Пробуем сделать ход через GameService
            boolean moveSuccessful = gameService.makeMove(placements);

            if (moveSuccessful) {
                // Размещаем фишки на доске
                for (TilePlacementDTO placement : placements) {
                    boardService.placeTile(placement);
                }

                // Считаем очки через ScoringService
                Board board = boardService.getBoard();
                int score = scoringService.countScore(board, placements);

                // Обновляем состояние через GameService
                gameService.processMove(room, session.getUserId(), placements, score);

                // Передаем ход
                game.setActivePlayerIdx((game.getActivePlayerIdx() + 1) % game.getPlayers().size());

                // Рассылаем обновление всем игрокам
                String boardState = serializeBoard(board);
                String playerState = serializePlayerState(currentPlayer);

                notifyRoomPlayers(room,
                        "MOVE_MADE|" + session.getUserId() + "|" + score + "|" +
                                boardState + "|" + playerState, null);

                // Уведомляем о следующем ходе
                Player nextPlayer = game.getPlayers().get(game.getActivePlayerIdx());
                notifyRoomPlayers(room, "NEXT_TURN|" + nextPlayer.getUserId() + "|90", null);

                // Проверяем условия окончания игры через GameService
                if (gameService.isGameFinished()) {
                    endGame(room);
                }

            } else {
                sendToClient(session.getChannel(), "ERROR|Недопустимый ход");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendToClient(session.getChannel(), "ERROR|Ошибка обработки хода: " + e.getMessage());
        }
    }

    private void handleReconnect(ClientSession session, String data) {
        try {
            int port = Integer.parseInt(data);

            // Используем RoomService для проверки возможности переподключения
            if (roomService.canReconnect(session.getUserId(), port)) {
                if (roomService.reconnectPlayer(session.getUserId(), port)) {
                    Room room = roomService.getRoomByPort(port);

                    // Восстанавливаем соединение в ConnectionManager
                    connectionManager.registerConnection(session.getUserId(), session.getChannel());

                    // Отправляем текущее состояние игры
                    String gameState;
                    if (room != null && room.isGameStarted()) {
                        gameState = gameService.serializeGameState(room);
                    } else {
                        gameState = "WAITING";
                    }

                    sendToClient(session.getChannel(), "RECONNECT_SUCCESS|" + gameState);

                    // Уведомляем оппонента
                    Long opponentId = room != null ? room.getOpponentId(session.getUserId()) : null;
                    if (opponentId != null) {
                        sendToUser(opponentId, "PLAYER_RECONNECTED|" + session.getUserId());
                    }
                } else {
                    sendToClient(session.getChannel(), "ERROR|Не удалось переподключиться");
                }
            } else {
                sendToClient(session.getChannel(), "ERROR|Время для переподключения истекло");
            }

        } catch (NumberFormatException e) {
            sendToClient(session.getChannel(), "ERROR|Неверный формат порта");
        }
    }

    private void handleClientDisconnect(SocketChannel channel) {
        ClientSession session = sessions.remove(channel);
        if (session == null) return;

        System.out.println("Клиент отключился: " + session.getUsername() +
                " на порту " + session.getPort());

        Long userId = session.getUserId();
        if (userId != null) {
            // Уведомляем RoomService о дисконнекте
            roomService.handlePlayerDisconnect(userId, session.getPort());

            // Удаляем из ConnectionManager
            connectionManager.removeConnection(channel);
        }

        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToClient(SocketChannel channel, String message) {
        if (channel != null && channel.isConnected()) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap((message + "\n").getBytes());
                channel.write(buffer);
            } catch (IOException e) {
                System.err.println("Ошибка отправки сообщения клиенту: " + e.getMessage());
            }
        }
    }

    private void sendToUser(Long userId, String message) {
        SocketChannel channel = connectionManager.getConnection(userId);
        if (channel != null) {
            sendToClient(channel, message);
        }
    }

    private void notifyRoomPlayers(Room room, String message, Long excludeUserId) {
        for (Long userId : room.getUsers()) {
            if (excludeUserId != null && userId.equals(excludeUserId)) continue;
            sendToUser(userId, message);
        }
    }

    // Вспомогательные методы
    private List<TilePlacementDTO> parseTilePlacements(String data) {
        List<TilePlacementDTO> placements = new ArrayList<>();
        if (data == null || data.isEmpty()) return placements;

        String[] placementsStr = data.split(";");
        for (String placement : placementsStr) {
            String[] parts = placement.split(",");
            if (parts.length == 3) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    char letter = parts[2].charAt(0);
                    placements.add(new TilePlacementDTO(x, y, letter));
                } catch (NumberFormatException e) {
                    System.err.println("Неверный формат координат: " + placement);
                }
            }
        }
        return placements;
    }

    private void endGame(Room room) {
        Game game = room.getGame();
        if (game == null) return;

        // Используем GameService для завершения игры
        gameService.endGameInRoom(room);

        // Получаем результаты из GameService
        String results = gameService.getGameResults(room);

        // Уведомляем игроков
        notifyRoomPlayers(room, "GAME_OVER|" + results, null);

        // Обновляем статистику пользователей
        for (Player player : game.getPlayers()) {
            User user = userService.findById(player.getUserId());
            if (user != null) {
                // Определяем победителя (в реальности GameService должен это делать)
                boolean isWin = false; // Заглушка
                userService.updateGames(player.getUserId(), isWin);
            }
        }

        // Очищаем игру в комнате
        room.setGame(null);
        room.setGameStarted(false);
    }

    // Сериализация состояния
    private String serializeBoard(Board board) {
        StringBuilder sb = new StringBuilder();
        BoardCell[][] cells = board.getBoardCells();

        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[y].length; x++) {
                BoardCell cell = cells[y][x];
                if (cell.getTile() != null) {
                    sb.append(x).append(",").append(y).append(",")
                            .append(cell.getTile().getLetter()).append(";");
                }
            }
        }
        return sb.toString();
    }

    private String serializePlayerState(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getUserId()).append(":")
                .append(player.getScore()).append(":")
                .append(player.getRack().size()).append(":");

        for (Tile tile : player.getRack()) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    // Фоновые задачи
    private void startBackgroundTasks() {
        // Очистка неактивных комнат каждые 30 секунд
        scheduler.scheduleAtFixedRate(() -> {
            try {
                roomService.cleanupInactiveRooms();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 30, 30, TimeUnit.SECONDS);

        // Проверка таймеров хода каждую секунду
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Получаем все комнаты и проверяем таймеры
                for (Room room : roomService.getAllRooms()) {
                    if (room.isGameStarted()) {
                        // Используем GameService для проверки таймеров
                        gameService.checkTurnTimers(room);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // Остальные обработчики команд
    private void handleLogout(ClientSession session) {
        if (session.isAuthenticated() && session.getUserId() != null) {
            Room room = roomService.getRoomByPort(session.getPort());
            if (room != null) {
                roomService.leaveRoom(session.getUserId(), session.getPort());
            }
            connectionManager.removeConnection(session.getChannel());
            session.setAuthenticated(false);
            sendToClient(session.getChannel(), "LOGOUT_SUCCESS");
        }
    }

    private void handleLeave(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        if (roomService.leaveRoom(session.getUserId(), session.getPort())) {
            sendToClient(session.getChannel(), "LEAVE_SUCCESS");
        } else {
            sendToClient(session.getChannel(), "ERROR|Не удалось покинуть комнату");
        }
    }

    private void handleSkip(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        if (gameService.skipTurn()) {
            sendToClient(session.getChannel(), "SKIP_SUCCESS");
            notifyRoomPlayers(room, "TURN_SKIPPED|" + session.getUserId(), null);
        } else {
            sendToClient(session.getChannel(), "ERROR|Не удалось пропустить ход");
        }
    }

    private void handleSurrender(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        // Помечаем игрока как сдавшегося
        Game game = room.getGame();
        if (game != null) {
            for (Player player : game.getPlayers()) {
                if (player.getUserId().equals(session.getUserId())) {
                    // Устанавливаем флаг сдачи
                    // В реальности это должно быть в Player модели
                    notifyRoomPlayers(room, "PLAYER_SURRENDERED|" + session.getUserId(), null);
                    endGame(room);
                    break;
                }
            }
        }
    }

    private void handleDrawOffer(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        // Используем GameService для обработки предложения ничьи
        gameService.processDrawOffer(room, session.getUserId());
        notifyRoomPlayers(room, "DRAW_OFFERED|" + session.getUserId(), session.getUserId());
    }

    private void handleDrawAccept(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        // Используем GameService для принятия ничьи
        gameService.acceptDrawOffer(room, session.getUserId());
        notifyRoomPlayers(room, "DRAW_ACCEPTED", null);
        endGame(room); // Завершаем игру как ничью
    }

    private void handleDrawReject(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null || !room.isGameStarted()) {
            sendToClient(session.getChannel(), "ERROR|Игра не активна");
            return;
        }

        notifyRoomPlayers(room, "DRAW_REJECTED|" + session.getUserId(), null);
    }

    private void handleGetStyle(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        try {
            int styleId = userService.getCurrentStyle(session.getUserId());
            sendToClient(session.getChannel(), "STYLE|" + styleId);
        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка получения стиля: " + e.getMessage());
        }
    }

    private void handleSetStyle(ClientSession session, String data) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        try {
            int styleId = Integer.parseInt(data);
            userService.updateStyle(session.getUserId(), styleId);
            sendToClient(session.getChannel(), "STYLE_UPDATED|" + styleId);
        } catch (NumberFormatException e) {
            sendToClient(session.getChannel(), "ERROR|Неверный формат стиля");
        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка обновления стиля: " + e.getMessage());
        }
    }

    private void handleGetAvailableStyles(ClientSession session) {
        try {
            List<Integer> styles = userService.getAvailableStyles();
            StringBuilder sb = new StringBuilder();
            sb.append("AVAILABLE_STYLES|");
            for (int i = 0; i < styles.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(styles.get(i));
            }
            sendToClient(session.getChannel(), sb.toString());
        } catch (Exception e) {
            sendToClient(session.getChannel(), "ERROR|Ошибка получения списка стилей: " + e.getMessage());
        }
    }

    private void handleListRooms(ClientSession session) {
        List<Room> rooms = roomService.getAllRooms();
        StringBuilder sb = new StringBuilder();
        sb.append("ROOM_LIST|");

        for (Room room : rooms) {
            if (!room.isGameStarted() && !room.isFull()) {
                sb.append(room.getPort()).append(",")
                        .append(room.getUserCount()).append(";");
            }
        }

        sendToClient(session.getChannel(), sb.toString());
    }

    private void handlePing(ClientSession session) {
        sendToClient(session.getChannel(), "PONG");
    }

    private void handleGetState(ClientSession session) {
        if (!session.isAuthenticated()) {
            sendToClient(session.getChannel(), "ERROR|Требуется авторизация");
            return;
        }

        Room room = roomService.getRoomByPort(session.getPort());
        if (room == null) {
            sendToClient(session.getChannel(), "ERROR|Комната не найдена");
            return;
        }

        String state;
        if (room.isGameStarted()) {
            state = gameService.serializeGameState(room);
        } else {
            state = "WAITING|" + room.getUserCount() + "|" +
                    (room.getHostId() != null ? room.getHostId() : "NONE");
        }

        sendToClient(session.getChannel(), "STATE|" + state);
    }
}