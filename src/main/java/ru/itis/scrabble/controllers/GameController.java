package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import ru.itis.scrabble.dto.GameStateDTO;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.models.*;
import ru.itis.scrabble.navigation.View;

import java.util.*;

public class GameController extends BaseController {

    // UI элементы из FXML
    @FXML private Label gameStatusLabel;
    @FXML private Label roomInfoLabel;
    @FXML private Label currentTurnPlayerLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar timerProgress;

    @FXML private HBox opponentTilesContainer;
    @FXML private Button confirmMoveButton;
    @FXML private Button skipTurnButton;
    @FXML private Button changeTilesButton;
    @FXML private Button surrenderButton;
    @FXML private Button offerDrawButton;

    @FXML private GridPane gameBoard;
    @FXML private Label myNameLabel;
    @FXML private Label myScoreLabel;
    @FXML private Label myLastMoveLabel;
    @FXML private Label opponentNameLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private Label opponentLastMoveLabel;
    @FXML private Label bagCountLabel;
    @FXML private Label turnCountLabel;
    @FXML private Label connectedLabel;
    @FXML private Button exitButton;

    // Игровое состояние
    private int roomPort;
    private GameStateDTO currentGameState;
    private BoardCellUI[][] boardCellsUI = new BoardCellUI[15][15];
    private List<TilePlacementDTO> currentPlacements = new ArrayList<>();
    private Map<Long, String> playerNames = new HashMap<>();
    private Map<Long, Integer> playerScores = new HashMap<>();
    private Map<Long, Integer> lastMoveScores = new HashMap<>();
    private Long currentPlayerId;
    private int turnCount = 1;
    private int timeLeft = 90;
    private Timer gameTimer;

    // Класс для отображения клетки доски
    private static class BoardCellUI {
        StackPane container;
        Rectangle background;
        Text letterText;
        Text pointsText;
        Tile currentTile;

        public BoardCellUI(StackPane container, Rectangle background,
                          Text letterText, Text pointsText) {
            this.container = container;
            this.background = background;
            this.letterText = letterText;
            this.pointsText = pointsText;
        }
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
        initializeBoard();
    }

    private void setupEventHandlers() {
        confirmMoveButton.setOnAction(event -> confirmMove());
        skipTurnButton.setOnAction(event -> skipTurn());
        changeTilesButton.setOnAction(event -> changeTiles());
        surrenderButton.setOnAction(event -> surrender());
        offerDrawButton.setOnAction(event -> offerDraw());
        exitButton.setOnAction(event -> exitGame());
    }

    @Override
    public void initData(Object data) {
        if (data instanceof Map) {
            Map<String, Object> gameData = (Map<String, Object>) data;
            roomPort = (int) gameData.get("roomPort");

            playerNames.put((Long) gameData.get("player1Id"),
                           (String) gameData.get("player1Name"));
            playerNames.put((Long) gameData.get("player2Id"),
                           (String) gameData.get("player2Name"));

            if (gameData.containsKey("gameState")) {
                updateGameState((GameStateDTO) gameData.get("gameState"));
            }

            roomInfoLabel.setText("Комната: " + roomPort);
            updatePlayerInfo();
            startGameTimer();

            // Запрашиваем актуальное состояние игры
            sendJsonCommand("GET_GAME_STATE", Map.of("roomPort", roomPort));
        }
    }

    private void initializeBoard() {
        // Создаем игровое поле 15x15
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                StackPane cellContainer = new StackPane();
                cellContainer.setPrefSize(35, 35);

                Rectangle bg = new Rectangle(30, 30);
                bg.setArcWidth(5);
                bg.setArcHeight(5);
                bg.setStyle("-fx-fill: #F5DEB3; -fx-stroke: #8B4513; -fx-stroke-width: 1;");

                Text letter = new Text("");
                letter.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                Text points = new Text("");
                points.setStyle("-fx-font-size: 8; -fx-fill: #666;");
                points.setTranslateY(10);

                cellContainer.getChildren().addAll(bg, letter, points);

                final int cellX = x;
                final int cellY = y;
                cellContainer.setOnMouseClicked(event -> handleCellClick(cellX, cellY));

                gameBoard.add(cellContainer, x, y);
                boardCellsUI[y][x] = new BoardCellUI(cellContainer, bg, letter, points);
            }
        }
    }

    private void handleCellClick(int x, int y) {
        if (!isMyTurn()) {
            navigator.showDialog("Не ваш ход", "Сейчас ход другого игрока");
            return;
        }

        // В реальной игре здесь будет выбор фишки из руки
        // Для демонстрации просто показываем диалог
        showTileSelectionDialog(x, y);
    }

    private void showTileSelectionDialog(int x, int y) {
        // Диалог выбора фишки (в реальной игре - из руки игрока)
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Размещение фишки");
        dialog.setHeaderText("Введите букву для клетки (" + x + ", " + y + ")");
        dialog.setContentText("Буква:");

        dialog.showAndWait().ifPresent(letter -> {
            if (letter.length() == 1) {
                char letterChar = letter.toUpperCase().charAt(0);
                try {
                    Tile tile = Tile.valueOf(String.valueOf(letterChar));
                    placeTile(tile, x, y);
                } catch (IllegalArgumentException e) {
                    navigator.showError("Ошибка", "Неверная буква");
                }
            }
        });
    }

    private void placeTile(Tile tile, int x, int y) {
        // Проверяем, свободна ли клетка
        if (boardCellsUI[y][x].currentTile != null) {
            navigator.showError("Ошибка", "Клетка уже занята");
            return;
        }

        // Добавляем размещение
        TilePlacementDTO placement = new TilePlacementDTO(tile, x, y);
        currentPlacements.add(placement);

        // Обновляем отображение
        updateCellUI(x, y, tile);

        // Отправляем предпросмотр на сервер (опционально)
        sendJsonCommand("TILE_PREVIEW", Map.of(
            "placements", currentPlacements,
            "roomPort", roomPort,
            "playerId", currentUserId
        ));
    }

    private void updateCellUI(int x, int y, Tile tile) {
        BoardCellUI cellUI = boardCellsUI[y][x];
        cellUI.currentTile = tile;
        cellUI.letterText.setText(String.valueOf(tile.getLetter()));
        cellUI.pointsText.setText(String.valueOf(tile.getPoints()));

        // Подсветка новой фишки
        cellUI.background.setStyle("-fx-fill: #4CAF50; -fx-stroke: #2E7D32; -fx-stroke-width: 2;");
    }

    private void confirmMove() {
        if (!isMyTurn()) {
            navigator.showError("Ошибка", "Сейчас не ваш ход");
            return;
        }

        if (currentPlacements.isEmpty()) {
            navigator.showError("Ошибка", "Не размещено ни одной фишки");
            return;
        }

        // Отправляем ход на сервер
        sendJsonCommand("MAKE_MOVE", Map.of(
            "roomPort", roomPort,
            "playerId", currentUserId,
            "placements", currentPlacements
        ));

        confirmMoveButton.setDisable(true);
        gameStatusLabel.setText("Ожидание проверки хода...");
    }

    private void skipTurn() {
        if (!isMyTurn()) {
            navigator.showError("Ошибка", "Сейчас не ваш ход");
            return;
        }

        sendJsonCommand("SKIP_TURN", Map.of(
            "roomPort", roomPort,
            "playerId", currentUserId
        ));
    }

    private void changeTiles() {
        if (!isMyTurn()) {
            navigator.showError("Ошибка", "Сейчас не ваш ход");
            return;
        }

        // Диалог выбора фишек для замены
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Замена фишек");
        dialog.setHeaderText("Введите буквы для замены (например: ABC)");
        dialog.setContentText("Буквы:");

        dialog.showAndWait().ifPresent(letters -> {
            if (!letters.isEmpty()) {
                sendJsonCommand("CHANGE_TILES", Map.of(
                    "roomPort", roomPort,
                    "playerId", currentUserId,
                    "letters", letters.toUpperCase()
                ));
            }
        });
    }

    private void surrender() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Сдача");
        alert.setHeaderText("Вы уверены, что хотите сдаться?");
        alert.setContentText("Это приведет к поражению в текущей игре.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendJsonCommand("SURRENDER", Map.of(
                    "roomPort", roomPort,
                    "playerId", currentUserId
                ));
            }
        });
    }

    private void offerDraw() {
        sendJsonCommand("OFFER_DRAW", Map.of(
            "roomPort", roomPort,
            "playerId", currentUserId
        ));

        navigator.showDialog("Предложение ничьей",
            "Вы предложили ничью. Ожидание ответа противника...");
    }

    private void exitGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Выход из игры");
        alert.setHeaderText("Вы уверены, что хотите выйти?");
        alert.setContentText("Это приведет к поражению в текущей игре.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendJsonCommand("EXIT_GAME", Map.of(
                    "roomPort", roomPort,
                    "playerId", currentUserId
                ));
                navigator.navigate(View.MAIN_MENU);
            }
        });
    }

    private void updateGameState(GameStateDTO gameState) {
        this.currentGameState = gameState;

        Platform.runLater(() -> {
            // Обновляем доску
            updateBoard(gameState.getBoard());

            // Обновляем информацию об игроках
            updatePlayersInfo(gameState.getPlayers());

            // Обновляем текущего игрока
            currentPlayerId = gameState.getPlayers().get(gameState.getCurrentPlayerIndex()).getUserId();
            currentTurnPlayerLabel.setText(playerNames.get(currentPlayerId));

            // Обновляем счетчик мешка
            bagCountLabel.setText("Осталось букв: " + gameState.getBagCount());

            // Обновляем счетчик ходов
            turnCountLabel.setText("Ход: " + turnCount);

            // Обновляем статус игры
            if (gameState.isGameOver()) {
                gameStatusLabel.setText("Игра завершена");
                endGame();
            } else {
                gameStatusLabel.setText(isMyTurn() ? "Ваш ход" : "Ход противника");
                updateButtonsState();
            }

            // Сбрасываем текущие размещения
            currentPlacements.clear();
            confirmMoveButton.setDisable(false);
        });
    }

    private void updateBoard(Board board) {
        BoardCell[][] cells = board.getBoardCells();
        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
                BoardCell cell = cells[y][x];
                BoardCellUI cellUI = boardCellsUI[y][x];

                if (cell.getTile() != null) {
                    cellUI.currentTile = cell.getTile();
                    cellUI.letterText.setText(String.valueOf(cell.getTile().getLetter()));
                    cellUI.pointsText.setText(String.valueOf(cell.getTile().getPoints()));

                    // Устанавливаем цвет в зависимости от типа клетки
                    switch (cell.getCellType()) {
                        case DWS -> cellUI.background.setStyle("-fx-fill: #FFB6C1;");
                        case TWS -> cellUI.background.setStyle("-fx-fill: #FF6347;");
                        case DLS -> cellUI.background.setStyle("-fx-fill: #ADD8E6;");
                        case TLS -> cellUI.background.setStyle("-fx-fill: #1E90FF;");
                        default -> cellUI.background.setStyle("-fx-fill: #F5DEB3;");
                    }
                } else {
                    cellUI.currentTile = null;
                    cellUI.letterText.setText("");
                    cellUI.pointsText.setText("");

                    // Восстанавливаем цвет клетки по типу
                    switch (cell.getCellType()) {
                        case DWS -> cellUI.background.setStyle("-fx-fill: #FFB6C1; -fx-stroke: #8B4513;");
                        case TWS -> cellUI.background.setStyle("-fx-fill: #FF6347; -fx-stroke: #8B4513;");
                        case DLS -> cellUI.background.setStyle("-fx-fill: #ADD8E6; -fx-stroke: #8B4513;");
                        case TLS -> cellUI.background.setStyle("-fx-fill: #1E90FF; -fx-stroke: #8B4513;");
                        default -> cellUI.background.setStyle("-fx-fill: #F5DEB3; -fx-stroke: #8B4513;");
                    }
                }
            }
        }
    }

    private void updatePlayersInfo(List<Player> players) {
        for (Player player : players) {
            playerScores.put(player.getUserId(), player.getScore());
            lastMoveScores.put(player.getUserId(), player.getLastPoints());

            if (player.getUserId().equals(currentUserId)) {
                myScoreLabel.setText("Всего очков: " + player.getScore());
                myLastMoveLabel.setText("Последний ход: " + player.getLastPoints());
                myNameLabel.setText(player.getUsername());
            } else {
                opponentScoreLabel.setText("Всего очков: " + player.getScore());
                opponentLastMoveLabel.setText("Последний ход: " + player.getLastPoints());
                opponentNameLabel.setText(player.getUsername());
            }
        }

        connectedLabel.setText("Игроков: " + players.size() + "/2");
    }

    private void updatePlayerInfo() {
        myNameLabel.setText(currentUsername);
    }

    private void updateButtonsState() {
        boolean myTurn = isMyTurn();
        confirmMoveButton.setDisable(!myTurn);
        skipTurnButton.setDisable(!myTurn);
        changeTilesButton.setDisable(!myTurn);
        offerDrawButton.setDisable(!myTurn);
    }

    private boolean isMyTurn() {
        return currentPlayerId != null && currentPlayerId.equals(currentUserId);
    }

    private void startGameTimer() {
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    timeLeft--;
                    timerLabel.setText(String.valueOf(timeLeft));
                    timerProgress.setProgress(timeLeft / 90.0);

                    if (timeLeft <= 0) {
                        // Время вышло
                        gameTimer.cancel();
                        if (isMyTurn()) {
                            sendJsonCommand("TIME_OUT", Map.of(
                                "roomPort", roomPort,
                                "playerId", currentUserId
                            ));
                        }
                    }
                });
            }
        }, 1000, 1000);
    }

    private void endGame() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // Переходим на экран окончания игры
        navigator.navigate(View.GAME_OVER, Map.of(
            "roomPort", roomPort,
            "playerScores", playerScores,
            "playerNames", playerNames,
            "winnerId", determineWinner()
        ));
    }

    private Long determineWinner() {
        // Определяем победителя по очкам
        Long winnerId = null;
        int maxScore = -1;

        for (Map.Entry<Long, Integer> entry : playerScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winnerId = entry.getKey();
            }
        }

        return winnerId;
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                if (message.startsWith("GAME_STATE_UPDATE|")) {
                    String json = message.substring("GAME_STATE_UPDATE|".length());
                    GameStateDTO gameState = mapper.readValue(json, GameStateDTO.class);
                    updateGameState(gameState);
                    turnCount++;

                } else if (message.startsWith("MOVE_ACCEPTED|")) {
                    String json = message.substring("MOVE_ACCEPTED|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    int score = ((Number) response.get("score")).intValue();
                    navigator.showDialog("Ход принят",
                        "Вы получили " + score + " очков!");

                } else if (message.startsWith("MOVE_REJECTED|")) {
                    String error = message.substring("MOVE_REJECTED|".length());
                    navigator.showError("Ход отклонен", error);
                    confirmMoveButton.setDisable(false);

                } else if (message.startsWith("TURN_SKIPPED|")) {
                    navigator.showDialog("Ход пропущен", "Вы пропустили ход");

                } else if (message.startsWith("TILES_CHANGED|")) {
                    navigator.showDialog("Фишки заменены", "Фишки успешно заменены");

                } else if (message.startsWith("DRAW_OFFERED|")) {
                    String json = message.substring("DRAW_OFFERED|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    String opponentName = (String) response.get("opponentName");

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Предложение ничьей");
                    alert.setHeaderText(opponentName + " предлагает ничью");
                    alert.setContentText("Принять предложение?");

                    alert.showAndWait().ifPresent(responseBtn -> {
                        boolean accept = responseBtn == ButtonType.OK;
                        sendJsonCommand("DRAW_RESPONSE", Map.of(
                            "roomPort", roomPort,
                            "playerId", currentUserId,
                            "accept", accept
                        ));
                    });

                } else if (message.startsWith("DRAW_ACCEPTED|")) {
                    navigator.showDialog("Ничья", "Оба игрока согласились на ничью");
                    endGame();

                } else if (message.startsWith("DRAW_REJECTED|")) {
                    navigator.showDialog("Ничья отклонена", "Противник отклонил предложение ничьей");

                } else if (message.startsWith("PLAYER_SURRENDERED|")) {
                    String json = message.substring("PLAYER_SURRENDERED|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    String playerName = (String) response.get("playerName");
                    navigator.showDialog("Сдача", playerName + " сдался!");
                    endGame();

                } else if (message.startsWith("GAME_OVER|")) {
                    String json = message.substring("GAME_OVER|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    Long winnerId = ((Number) response.get("winnerId")).longValue();
                    String winnerName = (String) response.get("winnerName");

                    navigator.showDialog("Игра окончена",
                        "Победитель: " + winnerName + "!\n" +
                        "Ваш счет: " + playerScores.get(currentUserId) + "\n" +
                        "Счет противника: " + playerScores.get(getOpponentId()));

                    endGame();

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);
                    confirmMoveButton.setDisable(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Long getOpponentId() {
        return playerNames.keySet().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);
    }

    public void cleanup() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
    }
}