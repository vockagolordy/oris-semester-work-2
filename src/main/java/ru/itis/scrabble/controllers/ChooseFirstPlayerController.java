package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import ru.itis.scrabble.navigation.View;

import java.util.Map;
import java.util.Random;

public class ChooseFirstPlayerController extends BaseController {

    @FXML private ProgressIndicator spinner;
    @FXML private VBox player1Card;
    @FXML private VBox player2Card;
    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;

    @FXML private VBox resultPanel;
    @FXML private Label firstPlayerLabel;
    @FXML private Label countdownLabel;
    @FXML private Button continueButton;

    private int roomPort;
    private String player1Name;
    private String player2Name;
    private Long player1Id;
    private Long player2Id;
    private boolean isDetermined = false;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        continueButton.setOnAction(event -> startGame());
    }

    @Override
    public void initData(Object data) {
        if (data instanceof Map) {
            Map<String, Object> gameData = (Map<String, Object>) data;
            roomPort = (int) gameData.get("roomPort");
            player1Id = (Long) gameData.get("hostId");
            player1Name = (String) gameData.get("hostName");
            player2Id = (Long) gameData.get("opponentId");
            player2Name = (String) gameData.get("opponentName");

            updatePlayerCards();
            determineFirstPlayer();
        }
    }

    private void updatePlayerCards() {
        player1NameLabel.setText(player1Name);
        player2NameLabel.setText(player2Name);
    }

    private void determineFirstPlayer() {
        // Симуляция определения первого игрока (случайный выбор)
        new Thread(() -> {
            try {
                // Показываем анимацию 3 секунды
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(100);
                }

                // Случайный выбор первого игрока
                Random random = new Random();
                boolean player1Starts = random.nextBoolean();
                Long firstPlayerId = player1Starts ? player1Id : player2Id;
                String firstPlayerName = player1Starts ? player1Name : player2Name;

                // Отправляем результат на сервер
                Map<String, Object> result = Map.of(
                    "roomPort", roomPort,
                    "firstPlayerId", firstPlayerId,
                    "firstPlayerName", firstPlayerName
                );

                sendJsonCommand("FIRST_PLAYER_DETERMINED", result);

                // Показываем результат в UI
                Platform.runLater(() -> {
                    showResult(firstPlayerName);
                    startCountdown();
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void showResult(String firstPlayerName) {
        spinner.setVisible(false);
        resultPanel.setVisible(true);
        firstPlayerLabel.setText(firstPlayerName);
        isDetermined = true;
    }

    private void startCountdown() {
        new Thread(() -> {
            try {
                for (int i = 5; i > 0; i--) {
                    final int count = i;
                    Platform.runLater(() -> countdownLabel.setText(String.valueOf(count)));
                    Thread.sleep(1000);
                }

                Platform.runLater(() -> {
                    countdownLabel.setVisible(false);
                    continueButton.setVisible(true);
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void startGame() {
        // Запрашиваем начало игры у сервера
        Map<String, Object> startMsg = Map.of(
            "type", "START_GAME_SESSION",
            "roomPort", roomPort
        );

        sendJsonCommand("START_GAME_SESSION", startMsg);
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                if (message.startsWith("GAME_SESSION_STARTED|")) {
                    // Игровая сессия создана, переходим в игру
                    String json = message.substring("GAME_SESSION_STARTED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    navigator.navigate(View.GAME, Map.of(
                        "roomPort", roomPort,
                        "player1Id", player1Id,
                        "player1Name", player1Name,
                        "player2Id", player2Id,
                        "player2Name", player2Name,
                        "gameState", response.get("gameState")
                    ));

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}