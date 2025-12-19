package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.util.DictUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServiceImpl implements GameService {
    private Board board;
    private Bag bag;
    private Game currentGame;
    private boolean gameFinished;

    // Таймеры ходов: userId -> время начала хода
    private final Map<Long, LocalDateTime> turnTimers = new ConcurrentHashMap<>();
    private static final int TURN_TIME_SECONDS = 90;
    private static final int CONSECUTIVE_SKIPS_TO_END = 4;

    // Счетчики пропусков ходов: userId -> количество подряд пропущенных ходов
    private final Map<Long, Integer> skipCounters = new ConcurrentHashMap<>();

    // Предложения ничьей: roomPort -> {offererUserId, expiryTime}
    private final Map<Integer, DrawOffer> drawOffers = new ConcurrentHashMap<>();

    public GameServiceImpl() {
        this.gameFinished = false;
    }

    @Override
    public void startGame() {
        this.gameFinished = false;
        this.skipCounters.clear();
        this.turnTimers.clear();
        this.drawOffers.clear();

        System.out.println("Новая игра начата");
    }

    @Override
    public void endGame() {
        this.gameFinished = true;
        this.skipCounters.clear();
        this.turnTimers.clear();
        this.drawOffers.clear();

        System.out.println("Игра завершена");
    }

    @Override
    public boolean isValidTilePosition(TilePlacementDTO tilePlacementDTO) {
        // Базовая проверка
        if (tilePlacementDTO == null || tilePlacementDTO.tile() == null) {
            return false;
        }

        // Проверка границ будет в BoardService
        // Здесь можно добавить дополнительные проверки

        return true;
    }

    @Override
    public boolean skipTurn() {
        if (currentGame == null || gameFinished) {
            return false;
        }

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return false;
        }

        Long userId = currentPlayer.getUserId();

        // Увеличиваем счетчик пропусков
        int skips = skipCounters.getOrDefault(userId, 0) + 1;
        skipCounters.put(userId, skips);

        // Сбрасываем счетчик для других игроков
        for (Player player : currentGame.getPlayers()) {
            if (!player.getUserId().equals(userId)) {
                skipCounters.put(player.getUserId(), 0);
            }
        }

        System.out.println("Игрок " + userId + " пропустил ход. Пропусков подряд: " + skips);
        return true;
    }

    @Override
    public boolean makeMove(List<TilePlacementDTO> placements) {
        if (currentGame == null || gameFinished || placements == null || placements.isEmpty()) {
            return false;
        }

        // Проверяем, что все фишки есть у текущего игрока
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return false;
        }

        // Проверяем, что все размещаемые фишки есть у игрока
        List<Tile> playerTiles = new ArrayList<>(currentPlayer.getRack());
        for (TilePlacementDTO placement : placements) {
            Tile tile = placement.tile();
            if (!playerTiles.contains(tile)) {
                System.out.println("У игрока нет фишки: " + tile.getLetter());
                return false;
            }
            playerTiles.remove(tile);
        }

        // Проверяем, что фишки образуют одно слово (горизонтально или вертикально)
        if (!isSingleWord(placements)) {
            System.out.println("Фишки не образуют одно слово");
            return false;
        }

        // Проверяем, что слово существует в словаре
        String formedWord = getFormedWord(placements);
        if (!DictUtil.isValidWord(formedWord)) {
            System.out.println("Слово не найдено в словаре: " + formedWord);
            return false;
        }

        // Проверяем, что это первый ход и проходит через центр
        if (isFirstMove() && !isThroughCenter(placements)) {
            System.out.println("Первый ход должен проходить через центр");
            return false;
        }

        // Проверяем, что все клетки между фишками заполнены
        if (!isContinuous(placements)) {
            System.out.println("Фишки должны быть размещены непрерывно");
            return false;
        }

        // Проверяем, что нет пересечений с существующими словами
        if (hasIntersections(placements)) {
            System.out.println("Запрещены пересечения с существующими словами");
            return false;
        }

        // Все проверки пройдены
        return true;
    }

    @Override
    public boolean isBagEmpty() {
        return bag != null && bag.isEmpty();
    }

    @Override
    public Board getBoard() {
        return board;
    }

    @Override
    public boolean isGameFinished() {
        if (gameFinished) {
            return true;
        }

        // Проверяем условия окончания игры

        // 1. Мешок пуст и оба игрока сделали по 4 пропуска подряд
        if (isBagEmpty()) {
            boolean allSkippedEnough = true;
            for (Player player : currentGame.getPlayers()) {
                int skips = skipCounters.getOrDefault(player.getUserId(), 0);
                if (skips < CONSECUTIVE_SKIPS_TO_END) {
                    allSkippedEnough = false;
                    break;
                }
            }
            if (allSkippedEnough) {
                System.out.println("Игра завершена: мешок пуст и все игроки пропустили по 4 хода");
                return true;
            }
        }

        // 2. У какого-то игрока закончились фишки
        for (Player player : currentGame.getPlayers()) {
            if (player.getRack().isEmpty()) {
                System.out.println("Игра завершена: у игрока " + player.getUserId() + " закончились фишки");
                return true;
            }
        }

        return false;
    }

    // === Дополнительные методы для интеграции с NetworkServer ===

    public void setCurrentGame(Game game) {
        this.currentGame = game;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public void setBag(Bag bag) {
        this.bag = bag;
    }

    public Player getCurrentPlayer() {
        if (currentGame == null || currentGame.getPlayers().isEmpty()) {
            return null;
        }

        int activeIndex = currentGame.getActivePlayerIdx();
        if (activeIndex < 0 || activeIndex >= currentGame.getPlayers().size()) {
            return null;
        }

        return currentGame.getPlayers().get(activeIndex);
    }

    public void startTurnTimer(Long userId) {
        turnTimers.put(userId, LocalDateTime.now());
    }

    public boolean isTurnTimeExpired(Long userId) {
        LocalDateTime startTime = turnTimers.get(userId);
        if (startTime == null) {
            return false;
        }

        long secondsElapsed = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        return secondsElapsed >= TURN_TIME_SECONDS;
    }

    public void checkTurnTimers(Room room) {
        if (room == null || !room.isGameStarted() || room.getGame() == null) {
            return;
        }

        Game game = room.getGame();
        Player currentPlayer = game.getPlayers().get(game.getActivePlayerIdx());

        if (currentPlayer != null && isTurnTimeExpired(currentPlayer.getUserId())) {
            System.out.println("Время хода истекло для игрока " + currentPlayer.getUserId());
            // Автоматически пропускаем ход
            skipTurn();
            // Передаем ход следующему игроку
            game.setActivePlayerIdx((game.getActivePlayerIdx() + 1) % game.getPlayers().size());
        }
    }

    public void processDrawOffer(Room room, Long offererId) {
        if (room == null) {
            return;
        }

        DrawOffer offer = new DrawOffer(offererId, LocalDateTime.now().plusSeconds(30));
        drawOffers.put(room.getPort(), offer);

        System.out.println("Игрок " + offererId + " предложил ничью в комнате " + room.getPort());
    }

    public boolean acceptDrawOffer(Room room, Long acceptorId) {
        if (room == null) {
            return false;
        }

        DrawOffer offer = drawOffers.get(room.getPort());
        if (offer == null || offer.isExpired()) {
            return false;
        }

        // Проверяем, что принимает не тот же игрок
        if (offer.offererId.equals(acceptorId)) {
            return false;
        }

        // Проверяем, что принимающий игрок находится в комнате
        if (!room.containsUser(acceptorId)) {
            return false;
        }

        System.out.println("Ничья принята в комнате " + room.getPort());
        drawOffers.remove(room.getPort());
        return true;
    }

    public void processMove(Room room, Long playerId, List<TilePlacementDTO> placements, int score) {
        if (room == null || room.getGame() == null) {
            return;
        }

        // Находим игрока и начисляем очки
        for (Player player : room.getGame().getPlayers()) {
            if (player.getUserId().equals(playerId)) {
                player.increaseScore(score);

                // Удаляем использованные фишки
                List<Tile> usedTiles = new ArrayList<>();
                for (TilePlacementDTO placement : placements) {
                    usedTiles.add(placement.tile());
                }
                player.removeTiles(usedTiles);

                // Добираем новые фишки
                if (bag != null && !bag.isEmpty()) {
                    int tilesToTake = Math.min(7 - player.getRack().size(), placements.size());
                    if (tilesToTake > 0) {
                        List<Tile> newTiles = new ArrayList<>();
                        for (int i = 0; i < tilesToTake; i++) {
                            Tile tile = bag.takeTiles();
                            if (tile != null) {
                                newTiles.add(tile);
                            }
                        }
                        player.addTiles(newTiles);
                    }
                }

                // Сбрасываем счетчик пропусков
                skipCounters.put(playerId, 0);

                System.out.println("Ход обработан для игрока " + playerId +
                        ", очки: " + score + ", новый счет: " + player.getScore());
                break;
            }
        }
    }

    public String serializeGameState(Room room) {
        if (room == null) {
            return "EMPTY";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PORT:").append(room.getPort()).append(";");
        sb.append("GAME_STARTED:").append(room.isGameStarted()).append(";");

        if (room.isGameStarted() && room.getGame() != null) {
            Game game = room.getGame();
            sb.append("ACTIVE_PLAYER:").append(game.getActivePlayerIdx()).append(";");
            sb.append("PLAYERS:[");

            for (int i = 0; i < game.getPlayers().size(); i++) {
                Player player = game.getPlayers().get(i);
                if (i > 0) sb.append(",");
                sb.append("{ID:").append(player.getUserId())
                        .append(",SCORE:").append(player.getScore())
                        .append(",TILES:").append(player.getRack().size())
                        .append("}");
            }
            sb.append("]");
        }

        return sb.toString();
    }

    public void endGameInRoom(Room room) {
        if (room != null) {
            room.setGameStarted(false);
            endGame();
        }
    }

    public String getGameResults(Room room) {
        if (room == null || room.getGame() == null) {
            return "NO_GAME";
        }

        List<Player> players = room.getGame().getPlayers();
        if (players.size() != 2) {
            return "INVALID_PLAYER_COUNT";
        }

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        // Определяем победителя
        if (player1.getScore() > player2.getScore()) {
            return "WINNER:" + player1.getUserId() +
                    "|SCORE1:" + player1.getScore() +
                    "|SCORE2:" + player2.getScore();
        } else if (player2.getScore() > player1.getScore()) {
            return "WINNER:" + player2.getUserId() +
                    "|SCORE1:" + player1.getScore() +
                    "|SCORE2:" + player2.getScore();
        } else {
            return "DRAW|SCORE1:" + player1.getScore() + "|SCORE2:" + player2.getScore();
        }
    }

    // === Вспомогательные методы для проверки ходов ===

    private boolean isFirstMove() {
        // Проверяем, пустое ли поле (первый ход)
        if (board == null) {
            return true;
        }

        BoardCell[][] cells = board.getBoardCells();
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                if (cells[i][j].getTile() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isSingleWord(List<TilePlacementDTO> placements) {
        if (placements.size() <= 1) {
            return true;
        }

        // Проверяем, все ли фишки на одной линии (горизонтально или вертикально)
        boolean sameRow = true;
        boolean sameCol = true;

        int firstRow = placements.get(0).y();
        int firstCol = placements.get(0).x();

        for (TilePlacementDTO placement : placements) {
            if (placement.y() != firstRow) {
                sameRow = false;
            }
            if (placement.x() != firstCol) {
                sameCol = false;
            }
        }

        return sameRow || sameCol;
    }

    private boolean isThroughCenter(List<TilePlacementDTO> placements) {
        int center = 7; // Для поля 15x15 центр на (7,7)

        for (TilePlacementDTO placement : placements) {
            if (placement.x() == center && placement.y() == center) {
                return true;
            }
        }

        return false;
    }

    private boolean isContinuous(List<TilePlacementDTO> placements) {
        if (placements.size() <= 1) {
            return true;
        }

        // Сортируем по координатам
        List<TilePlacementDTO> sorted = new ArrayList<>(placements);
        sorted.sort((a, b) -> {
            if (a.y() != b.y()) {
                return Integer.compare(a.y(), b.y());
            }
            return Integer.compare(a.x(), b.x());
        });

        // Проверяем непрерывность
        for (int i = 1; i < sorted.size(); i++) {
            TilePlacementDTO prev = sorted.get(i - 1);
            TilePlacementDTO curr = sorted.get(i);

            // Проверяем горизонтальную непрерывность
            if (prev.y() == curr.y() && curr.x() - prev.x() > 1) {
                // Проверяем, заполнены ли промежуточные клетки
                for (int x = prev.x() + 1; x < curr.x(); x++) {
                    if (getTileAt(x, prev.y()) == null) {
                        return false;
                    }
                }
            }
            // Проверяем вертикальную непрерывность
            else if (prev.x() == curr.x() && curr.y() - prev.y() > 1) {
                // Проверяем, заполнены ли промежуточные клетки
                for (int y = prev.y() + 1; y < curr.y(); y++) {
                    if (getTileAt(prev.x(), y) == null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean hasIntersections(List<TilePlacementDTO> placements) {
        // В текущей реализации запрещаем любые пересечения
        // Можно расширить для поддержки классических правил Scrabble
        return false;
    }

    private String getFormedWord(List<TilePlacementDTO> placements) {
        if (placements.isEmpty()) {
            return "";
        }

        StringBuilder word = new StringBuilder();

        // Сортируем фишки в порядке их расположения в слове
        List<TilePlacementDTO> sorted = new ArrayList<>(placements);
        boolean isHorizontal = true;

        if (placements.size() > 1) {
            isHorizontal = placements.get(0).y() == placements.get(1).y();
        }

        if (isHorizontal) {
            sorted.sort(Comparator.comparingInt(TilePlacementDTO::x));
        } else {
            sorted.sort(Comparator.comparingInt(TilePlacementDTO::y));
        }

        for (TilePlacementDTO placement : sorted) {
            word.append(placement.tile().getLetter());
        }

        return word.toString();
    }

    private Tile getTileAt(int x, int y) {
        if (board == null) {
            return null;
        }

        BoardCell[][] cells = board.getBoardCells();
        if (y < 0 || y >= cells.length || x < 0 || x >= cells[y].length) {
            return null;
        }

        return cells[y][x].getTile();
    }

    // Внутренний класс для предложений ничьей
    private static class DrawOffer {
        final Long offererId;
        final LocalDateTime expiryTime;

        DrawOffer(Long offererId, LocalDateTime expiryTime) {
            this.offererId = offererId;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}