package ru.itis.scrabble.services;

import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import java.util.*;

public class GameServiceImpl implements GameService {
    private final BagService bagService;
    private final BoardService boardService;
    private final ScoringService scoringService;
    private Game game;
    private final List<User> users;
    private boolean gameStarted;
    private int consecutiveSkips;
    private static final int MAX_CONSECUTIVE_SKIPS = 3;

    public GameServiceImpl(List<User> users) {
        this.users = users;
        this.bagService = new BagServiceImpl();
        this.boardService = new BoardServiceImpl();
        this.scoringService = new ScoringServiceImpl();
        this.game = null;
        this.gameStarted = false;
        this.consecutiveSkips = 0;
    }

    private List<Player> createPlayers(List<User> users) {
        List<Player> players = new ArrayList<>();
        for (User user : users) {
            players.add(new Player(user.getId(), user.getUsername()));
        }
        return players;
    }

    @Override
    public void startGame() {
        if (gameStarted) {
            throw new IllegalStateException("Игра уже начата");
        }

        // Создаем игроков
        List<Player> players = createPlayers(users);

        // Создаем игру
        game = new Game(players, 0);

        // Инициализируем мешок
        bagService.fullBag();

        // Создаем доску
        boardService.createBoard();

        // Раздаем начальные фишки игрокам (по 7 фишек каждому)
        for (Player player : players) {
            List<Tile> initialTiles = bagService.takeTiles(7);
            player.addTiles(initialTiles);
        }

        gameStarted = true;
        consecutiveSkips = 0;

    }

    @Override
    public void endGame() {
        if (!gameStarted) {
            throw new IllegalStateException("Игра не начата");
        }

        // Подсчитываем финальные очки
        calculateFinalScores();

        // Определяем победителя
        Player winner = determineWinner();

        // Обновляем статистику пользователей
        updateUserStats(winner);

        // Сбрасываем состояние
        game = null;
        gameStarted = false;
        consecutiveSkips = 0;

    }

    @Override
    public boolean isValidTilePosition(TilePlacementDTO tilePlacementDTO) {
        if (!gameStarted || game == null) {
            return false;
        }

        // Проверяем через BoardService
        return boardService.isValidTilePosition(tilePlacementDTO);
    }

    @Override
    public boolean skipTurn() {
        if (!gameStarted || game == null) {
            return false;
        }

        // Увеличиваем счетчик пропущенных ходов
        consecutiveSkips++;

        // Передаем ход следующему игроку
        passTurnToNextPlayer();

        // Проверяем, не пора ли завершить игру
        if (consecutiveSkips >= MAX_CONSECUTIVE_SKIPS) {
            endGame();
            return true;
        }

        return true;
    }

    @Override
    public boolean makeMove(List<TilePlacementDTO> tilePlacements) {
        if (!gameStarted || game == null) {
            return false;
        }

        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return false;
        }

        // Проверяем валидность всех позиций
        for (TilePlacementDTO placement : tilePlacements) {
            if (!boardService.isValidTilePosition(placement)) {
                System.out.println("Невалидная позиция: " + placement.x() + "," + placement.y());
                return false;
            }

            // Проверяем, что у игрока есть эта фишка
            if (!playerHasTile(currentPlayer, placement.tile())) {
                System.out.println("У игрока нет фишки: " + placement.tile());
                return false;
            }
        }

        // Проверяем правила размещения (все фишки на одной линии)
        if (!isValidPlacement(tilePlacements)) {
            System.out.println("Фишки размещены не по правилам");
            return false;
        }

//        // Проверяем, что первое слово проходит через центр
//        if (boardService.isFirstMove() && !isCrossingCenter(tilePlacements)) {
//            System.out.println("Первое слово должно проходить через центр");
//            return false;
//        }
//
//        // Проверяем, что фишки соприкасаются с существующими (если это не первый ход)
//        if (!boardService.isFirstMove() && !isTouchingExistingTiles(tilePlacements)) {
//            System.out.println("Фишки должны соприкасаться с существующими");
//            return false;
//        }

        // Получаем текущую доску
        Board board = boardService.getBoard();
        if (board == null) {
            return false;
        }

        // Подсчитываем очки (передаем Board в ScoringService)
        int score = scoringService.countScore(board, tilePlacements);

        // Размещаем фишки на доске
        for (TilePlacementDTO placement : tilePlacements) {
            boardService.placeTile(placement);
        }

        // Убираем использованные фишки у игрока
        removeTilesFromPlayer(currentPlayer, tilePlacements);

        // Добавляем очки игроку
        currentPlayer.increaseScore(score);

        // Добираем новые фишки игроку
        List<Tile> newTiles = bagService.takeTiles(tilePlacements.size());
        currentPlayer.addTiles(newTiles);

        // Сбрасываем счетчик пропусков
        consecutiveSkips = 0;

        // Передаем ход следующему игроку
        passTurnToNextPlayer();

        System.out.println("Ход выполнен. Очков заработано: " + score);

        // Проверяем, не закончилась ли игра
        if (isGameFinished()) {
            endGame();
        }

        return true;
    }

    @Override
    public boolean isBagEmpty() {
        return bagService.isEmpty();
    }

    @Override
    public Board getBoard() {
        return boardService.getBoard();
    }

    @Override
    public boolean isGameFinished() {
        if (!gameStarted || game == null) {
            return false;
        }

        // Игра заканчивается, если:
        // 1. Мешок пуст и один из игроков использовал все фишки
        // 2. Игроки пропустили ход MAX_CONSECUTIVE_SKIPS раз подряд

        if (bagService.isEmpty()) {
            for (Player player : game.getPlayers()) {
                if (player.getRack().isEmpty()) {
                    return true;
                }
            }
        }

        return consecutiveSkips >= MAX_CONSECUTIVE_SKIPS;
    }

    // Вспомогательные методы

    private Player getCurrentPlayer() {
        if (game == null) return null;
        int idx = game.getActivePlayerIdx();
        if (idx >= 0 && idx < game.getPlayers().size()) {
            return game.getPlayers().get(idx);
        }
        return null;
    }

    private void passTurnToNextPlayer() {
        if (game == null) return;

        int currentIdx = game.getActivePlayerIdx();
        int nextIdx = (currentIdx + 1) % game.getPlayers().size();
        game.setActivePlayerIdx(nextIdx);
    }

    private boolean playerHasTile(Player player, Tile tile) {
        return player.getRack().contains(tile);
    }

    private void removeTilesFromPlayer(Player player, List<TilePlacementDTO> placements) {
        List<Tile> tilesToRemove = new ArrayList<>();
        for (TilePlacementDTO placement : placements) {
            tilesToRemove.add(placement.tile());
        }
        player.removeTiles(tilesToRemove);
    }

    private boolean isValidPlacement(List<TilePlacementDTO> placements) {
        if (placements.isEmpty()) return false;

        // Проверяем, что все фишки на одной горизонтальной или вертикальной линии
        boolean sameRow = true;
        boolean sameColumn = true;

        int firstRow = placements.get(0).y();
        int firstColumn = placements.get(0).x();

        for (TilePlacementDTO placement : placements) {
            if (placement.y() != firstRow) {
                sameRow = false;
            }
            if (placement.x() != firstColumn) {
                sameColumn = false;
            }
        }

        // Если не все на одной линии, проверяем смежные позиции
        if (!sameRow && !sameColumn) {
            return false;
        }

        // Если на одной линии, проверяем что нет пропусков
        if (sameRow) {
            List<Integer> columns = new ArrayList<>();
            for (TilePlacementDTO placement : placements) {
                columns.add(placement.x());
            }
            Collections.sort(columns);

            for (int i = 1; i < columns.size(); i++) {
                if (columns.get(i) - columns.get(i - 1) > 1) {
                    return false; // Есть пропуски
                }
            }
        } else { // sameColumn
            List<Integer> rows = new ArrayList<>();
            for (TilePlacementDTO placement : placements) {
                rows.add(placement.y());
            }
            Collections.sort(rows);

            for (int i = 1; i < rows.size(); i++) {
                if (rows.get(i) - rows.get(i - 1) > 1) {
                    return false; // Есть пропуски
                }
            }
        }

        return true;
    }

    private boolean isCrossingCenter(List<TilePlacementDTO> placements) {
        int center = 7; // BOARD_SIZE / 2
        for (TilePlacementDTO placement : placements) {
            if (placement.x() == center && placement.y() == center) {
                return true;
            }
        }
        return false;
    }

    private boolean isTouchingExistingTiles(List<TilePlacementDTO> placements) {
        // Проверяем, что хотя бы одна фишка соприкасается с существующей
        for (TilePlacementDTO placement : placements) {
            int x = placement.x();
            int y = placement.y();

            // Проверяем соседние клетки
            if (boardService.getTileAt(x-1, y) != null ||
                boardService.getTileAt(x+1, y) != null ||
                boardService.getTileAt(x, y-1) != null ||
                boardService.getTileAt(x, y+1) != null) {
                return true;
            }
        }
        return false;
    }

    private void calculateFinalScores() {
        if (game == null) return;
    }

    private Player determineWinner() {
        if (game == null) return null;

        Player winner = null;
        int maxScore = Integer.MIN_VALUE;

        for (Player player : game.getPlayers()) {
            if (player.getScore() > maxScore) {
                maxScore = player.getScore();
                winner = player;
            }
        }

        return winner;
    }

    private void updateUserStats(Player winner) {
        // Здесь должна быть логика обновления статистики пользователей в БД
        // Пока просто выводим в консоль
        System.out.println("Победитель: игрок с ID " + (winner != null ? winner.getUserId() : "не определен"));

        // В реальной реализации:
        // 1. Найти User по winner.getUserId()
        // 2. Вызвать user.addWin()
        // 3. Для проигравших: user.addLose()
        // 4. Сохранить в репозитории
    }

    // Дополнительные методы для удобства

    public Player getCurrentPlayerInfo() {
        return getCurrentPlayer();
    }

    public List<Player> getAllPlayers() {
        return game != null ? game.getPlayers() : Collections.emptyList();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public int getRemainingTilesCount() {
        // В реальной реализации нужно получить из BagService
        // Пока возвращаем примерное значение
        return bagService.isEmpty() ? 0 : 50;
    }
}