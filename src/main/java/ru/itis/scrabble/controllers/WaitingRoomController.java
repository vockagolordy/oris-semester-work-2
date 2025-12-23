package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import ru.itis.scrabble.navigation.View;
import ru.itis.scrabble.dto.NetworkMessageDTO;

import java.util.*;

public class WaitingRoomController extends BaseController {

    @FXML private Label roomInfoLabel;
    @FXML private ListView<String> playersListView;
    @FXML private Label readyStatusLabel;
    @FXML private Button readyButton;
    @FXML private Button startButton;
    @FXML private Button leaveButton;
    @FXML private VBox reconnectPanel;
    @FXML private Button reconnectButton;
    @FXML private Label waitingTimerLabel;

    private ObservableList<String> players = FXCollections.observableArrayList();
    private int roomPort;
    private Long hostId;
    private String hostName;
    private String opponentName;
    private Long opponentId;
    private boolean isHost = false;
    private boolean isReady = false;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        playersListView.setItems(players);
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        readyButton.setOnAction(_ -> toggleReady());
        startButton.setOnAction(_ -> startGame());
        leaveButton.setOnAction(_ -> leaveRoom());
        reconnectButton.setOnAction(_ -> reconnectToGame());
    }

    @Override
    public void initData(Object data) {
        if (data instanceof Map) {
            Map<String, Object> roomData = (Map<String, Object>) data;
            roomPort = (int) roomData.get("port");
            hostId = (Long) roomData.get("hostId");
            hostName = (String) roomData.get("hostName");
            isHost = hostId.equals(currentUserId);

            if (roomData.containsKey("opponentName")) {
                opponentName = (String) roomData.get("opponentName");
                opponentId = (Long) roomData.get("opponentId");
            }

            updateUI();

            // Отправляем сообщение о входе в комнату
            Map<String, Object> joinMsg = Map.of(
                "type", "JOIN_ROOM",
                "roomPort", roomPort,
                "userId", currentUserId,
                "username", currentUsername
            );
            sendNetworkMessage("ROOM_JOIN", joinMsg);

            // Запрашиваем обновленную информацию о комнате
            sendNetworkMessage("GET_ROOM_INFO", Map.of("port", roomPort));

            startTimer();
        }
    }

    private void updateUI() {
        roomInfoLabel.setText("Комната порт: " + roomPort);

        players.clear();
        players.add(hostName + " (Хост)");
        if (opponentName != null) {
            players.add(opponentName);
        }

        if (isHost) {
            readyButton.setDisable(false);
            startButton.setDisable(false);
            readyStatusLabel.setText("Вы хост комнаты");
        } else {
            readyButton.setDisable(false);
            startButton.setDisable(true); // Не хост не может начать игру
            readyStatusLabel.setText("Ожидание готовности хоста...");
        }

        // Проверяем, есть ли сохраненная игра
        reconnectPanel.setVisible(false); // По умолчанию скрываем
    }

    private void toggleReady() {
        isReady = !isReady;

        Map<String, Object> readyMsg = Map.of(
            "type", "PLAYER_READY",
            "roomPort", roomPort,
            "userId", currentUserId,
            "isReady", isReady
        );

        sendNetworkMessage("PLAYER_READY", readyMsg);

        if (isReady) {
            readyButton.setText("Не готов");
            readyButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        } else {
            readyButton.setText("Готов");
            readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        }
    }

    private void startGame() {
        if (!isHost) {
            navigator.showError("Ошибка", "Только хост может начать игру");
            return;
        }

        Map<String, Object> startMsg = Map.of(
            "type", "START_GAME",
            "roomPort", roomPort,
            "hostId", hostId
        );

        sendNetworkMessage("START_GAME", startMsg);

        // Показываем индикатор загрузки
        readyStatusLabel.setText("Начинаем игру...");
        startButton.setDisable(true);
    }

    private void leaveRoom() {
        Map<String, Object> leaveMsg = Map.of(
            "type", "LEAVE_ROOM",
            "roomPort", roomPort,
            "userId", currentUserId
        );

        sendNetworkMessage("LEAVE_ROOM", leaveMsg);

        navigator.navigate(View.MAIN_MENU);
    }

    private void reconnectToGame() {
        Map<String, Object> reconnectMsg = Map.of(
            "type", "RECONNECT_GAME",
            "roomPort", roomPort,
            "userId", currentUserId
        );

        sendNetworkMessage("RECONNECT", reconnectMsg);
    }

    private void startTimer() {
        new Thread(() -> {
            for (int seconds = 600; seconds > 0; seconds--) { // 10 минут
                final int mins = seconds / 60;
                final int secs = seconds % 60;

                Platform.runLater(() -> {
                    waitingTimerLabel.setText(String.format("Таймер: %02d:%02d", mins, secs));
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    public void handleNetworkMessage(NetworkMessageDTO message) {
        Platform.runLater(() -> {
            try {
                String payload = message.payload();
                if (payload.startsWith("PLAYER_JOINED|")) {
                    String json = payload.substring("PLAYER_JOINED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    opponentName = (String) response.get("username");
                    opponentId = ((Number) response.get("userId")).longValue();

                    updateUI();

                } else if (payload.startsWith("PLAYER_LEFT|")) {
                    String json = payload.substring("PLAYER_LEFT|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    opponentName = null;
                    opponentId = null;

                    updateUI();
                    navigator.showDialog("Игрок вышел", "Другой игрок покинул комнату");

                } else if (payload.startsWith("PLAYER_READY_CHANGED|")) {
                    String json = payload.substring("PLAYER_READY_CHANGED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    Long playerId = ((Number) response.get("userId")).longValue();
                    boolean playerReady = (boolean) response.get("isReady");

                    if (playerId.equals(opponentId)) {
                        readyStatusLabel.setText(
                            opponentName + " " + (playerReady ? "готов" : "не готов")
                        );
                    }

                } else if (payload.startsWith("GAME_STARTING|")) {
                    // Игра начинается, переходим к выбору первого игрока
                    navigator.navigate(View.CHOOSE_FIRST_PLAYER, Map.of(
                        "roomPort", roomPort,
                        "hostId", hostId,
                        "hostName", hostName,
                        "opponentId", opponentId,
                        "opponentName", opponentName
                    ));

                } else if (payload.startsWith("ROOM_CLOSED|")) {
                    navigator.showError("Комната закрыта", "Хост закрыл комнату");
                    navigator.navigate(View.MAIN_MENU);

                } else if (payload.startsWith("RECONNECT_AVAILABLE|")) {
                    // Есть сохраненная игра
                    reconnectPanel.setVisible(true);

                } else if (payload.startsWith("ERROR|")) {
                    String error = payload.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}