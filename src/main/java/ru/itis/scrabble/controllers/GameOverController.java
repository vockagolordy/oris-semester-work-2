package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import ru.itis.scrabble.navigation.View;

import java.util.Map;

public class GameOverController extends BaseController {

    @FXML private VBox victoryPanel;
    @FXML private VBox defeatPanel;
    @FXML private VBox drawPanel;

    @FXML private Label player1NameLabel;
    @FXML private Label player1ScoreLabel;
    @FXML private Label player2NameLabel;
    @FXML private Label player2ScoreLabel;

    @FXML private Label totalMovesLabel;
    @FXML private Label durationLabel;
    @FXML private Label longestWordLabel;
    @FXML private Label bestMoveLabel;

    @FXML private Button playAgainButton;
    @FXML private Button mainMenuButton;
    @FXML private Button statsButton;

    private int roomPort;
    private Long winnerId;
    private Map<Long, String> playerNames;
    private Map<Long, Integer> playerScores;
    private Long player1Id;
    private Long player2Id;
    private int totalMoves = 0;
    private String gameDuration = "0:00";
    private String longestWord = "-";
    private int bestMoveScore = 0;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        playAgainButton.setOnAction(event -> playAgain());
        mainMenuButton.setOnAction(event -> goToMainMenu());
        statsButton.setOnAction(event -> showStatistics());
    }

    @Override
    public void initData(Object data) {
        if (data instanceof Map) {
            Map<String, Object> gameData = (Map<String, Object>) data;
            roomPort = (int) gameData.get("roomPort");
            playerScores = (Map<Long, Integer>) gameData.get("playerScores");
            playerNames = (Map<Long, String>) gameData.get("playerNames");
            winnerId = (Long) gameData.get("winnerId");

            // Определяем ID игроков
            player1Id = playerNames.keySet().stream().findFirst().orElse(null);
            player2Id = playerNames.keySet().stream()
                    .filter(id -> !id.equals(player1Id))
                    .findFirst().orElse(null);

            // Если есть дополнительная статистика
            if (gameData.containsKey("totalMoves")) {
                totalMoves = (int) gameData.get("totalMoves");
            }
            if (gameData.containsKey("gameDuration")) {
                gameDuration = (String) gameData.get("gameDuration");
            }
            if (gameData.containsKey("longestWord")) {
                longestWord = (String) gameData.get("longestWord");
            }
            if (gameData.containsKey("bestMoveScore")) {
                bestMoveScore = (int) gameData.get("bestMoveScore");
            }

            updateUI();

            // Отправляем запрос на завершение игры на сервер
            sendGameOverToServer();
        }
    }

    private void updateUI() {
        // Определяем, кто победил
        boolean isWinner = winnerId != null && winnerId.equals(currentUserId);
        boolean isDraw = winnerId == null ||
                        (playerScores.get(player1Id) != null &&
                         playerScores.get(player2Id) != null &&
                         playerScores.get(player1Id).equals(playerScores.get(player2Id)));

        // Показываем соответствующий результат
        if (isDraw) {
            victoryPanel.setVisible(false);
            defeatPanel.setVisible(false);
            drawPanel.setVisible(true);
        } else if (isWinner) {
            victoryPanel.setVisible(true);
            defeatPanel.setVisible(false);
            drawPanel.setVisible(false);
        } else {
            victoryPanel.setVisible(false);
            defeatPanel.setVisible(true);
            drawPanel.setVisible(false);
        }

        // Обновляем информацию об игроках
        if (player1Id != null && playerNames.containsKey(player1Id)) {
            player1NameLabel.setText(playerNames.get(player1Id));
            player1ScoreLabel.setText(String.valueOf(playerScores.getOrDefault(player1Id, 0)));
        }

        if (player2Id != null && playerNames.containsKey(player2Id)) {
            player2NameLabel.setText(playerNames.get(player2Id));
            player2ScoreLabel.setText(String.valueOf(playerScores.getOrDefault(player2Id, 0)));
        }

        // Обновляем статистику
        totalMovesLabel.setText(String.valueOf(totalMoves));
        durationLabel.setText(gameDuration);
        longestWordLabel.setText(longestWord);
        bestMoveLabel.setText(String.valueOf(bestMoveScore));
    }

    private void sendGameOverToServer() {
        Map<String, Object> gameOverData = Map.of(
            "roomPort", roomPort,
            "winnerId", winnerId,
            "playerScores", playerScores,
            "currentUserId", currentUserId
        );

        sendJsonCommand("GAME_OVER_CONFIRMED", gameOverData);
    }

    private void playAgain() {
        // Отправляем запрос на повторную игру
        Map<String, Object> playAgainData = Map.of(
            "roomPort", roomPort,
            "playerId", currentUserId
        );

        sendJsonCommand("PLAY_AGAIN_REQUEST", playAgainData);

        playAgainButton.setDisable(true);
        playAgainButton.setText("Ожидание ответа...");
    }

    private void goToMainMenu() {
        // Отправляем сообщение о выходе из комнаты
        if (roomPort > 0) {
            sendJsonCommand("LEAVE_ROOM_AFTER_GAME", Map.of(
                "roomPort", roomPort,
                "userId", currentUserId
            ));
        }

        navigator.navigate(View.MAIN_MENU);
    }

    private void showStatistics() {
        // Переходим на экран профиля для просмотра статистики
        navigator.navigate(View.PROFILE);
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                if (message.startsWith("PLAY_AGAIN_ACCEPTED|")) {
                    // Оба игрока согласились играть снова
                    String json = message.substring("PLAY_AGAIN_ACCEPTED|".length());
                    Map<String, Object> response = new ObjectMapper().readValue(json, Map.class);

                    int newRoomPort = (int) response.get("newRoomPort");
                    Long opponentId = ((Number) response.get("opponentId")).longValue();
                    String opponentName = (String) response.get("opponentName");

                    // Переходим в комнату ожидания новой игры
                    navigator.navigate(View.WAITING_ROOM, Map.of(
                        "port", newRoomPort,
                        "hostId", currentUserId,
                        "hostName", currentUsername,
                        "opponentId", opponentId,
                        "opponentName", opponentName
                    ));

                } else if (message.startsWith("PLAY_AGAIN_REJECTED|")) {
                    // Противник отказался играть снова
                    String opponentName = message.substring("PLAY_AGAIN_REJECTED|".length());
                    navigator.showDialog("Повторная игра",
                        opponentName + " отказался играть снова");
                    playAgainButton.setDisable(false);
                    playAgainButton.setText("Играть снова");

                } else if (message.startsWith("STATISTICS_UPDATED|")) {
                    // Статистика обновлена на сервере
                    System.out.println("Статистика игрока обновлена");

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);
                    playAgainButton.setDisable(false);
                    playAgainButton.setText("Играть снова");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}