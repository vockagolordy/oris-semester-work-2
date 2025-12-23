package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.itis.scrabble.navigation.View;
import ru.itis.scrabble.dto.NetworkMessageDTO;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;

public class CreateRoomController extends BaseController {

    @FXML
    private TextField portField;
    @FXML
    private Button randomPortButton;
    @FXML
    private Button createButton;
    @FXML
    private Button cancelButton;
    @FXML
    private VBox waitingPanel;
    @FXML
    private Label waitingTimerLabel;
    @FXML
    private Label roomPortLabel;
    @FXML
    private Button cancelWaitingButton;
    @FXML
    private Label errorLabel;

    private boolean waitingForPlayer = false;
    private int roomPort;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
        generateRandomPort();
    }

    private void setupEventHandlers() {
        randomPortButton.setOnAction(_ -> generateRandomPort());
        createButton.setOnAction(_ -> createRoom());
        cancelButton.setOnAction(_ -> navigator.navigate(View.MAIN_MENU));
        cancelWaitingButton.setOnAction(_ -> cancelWaiting());
    }

    private void generateRandomPort() {
        Random random = new Random();
        int port = 8000 + random.nextInt(2000); // Порт от 8000 до 9999
        portField.setText(String.valueOf(port));
    }

    private void createRoom() {
        try {
            roomPort = Integer.parseInt(portField.getText().trim());

            if (roomPort < 1024 || roomPort > 65535) {
                showError("Порт должен быть в диапазоне 1024-65535");
                return;
            }

            createButton.setDisable(true);
            errorLabel.setVisible(false);

            // Отправляем запрос на создание комнаты
            Map<String, Object> roomData = Map.of(
                    "port", roomPort,
                    "creatorId", currentUserId,
                    "creatorName", currentUsername
            );

            sendNetworkMessage("CREATE_ROOM", roomData);

        } catch (NumberFormatException e) {
            showError("Введите корректный номер порта");
        }
    }

    private void showWaitingPanel() {
        waitingPanel.setVisible(true);
        roomPortLabel.setText("Порт: " + roomPort);
        startWaitingTimer();
    }

    private void hideWaitingPanel() {
        waitingPanel.setVisible(false);
        waitingForPlayer = false;
        createButton.setDisable(false);
    }

    private void startWaitingTimer() {
        // Таймер ожидания (10 минут)
        new Thread(() -> {
            for (int seconds = 600; seconds > 0 && waitingForPlayer; seconds--) {
                final int mins = seconds / 60;
                final int secs = seconds % 60;

                Platform.runLater(() -> {
                    waitingTimerLabel.setText(String.format("Ожидание: %02d:%02d", mins, secs));
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (waitingForPlayer) {
                Platform.runLater(() -> {
                    hideWaitingPanel();
                    navigator.showError("Время ожидания истекло", "Второй игрок не подключился");
                });
            }
        }).start();
    }

    private void cancelWaiting() {
        waitingForPlayer = false;
        // Отправляем команду отмены ожидания
        sendNetworkMessage("CANCEL_WAITING", Map.of("port", roomPort));
        hideWaitingPanel();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @Override
    public void handleNetworkMessage(NetworkMessageDTO message) {
        Platform.runLater(() -> {
            try {
                String payload = message.payload();
                if (payload.startsWith("ROOM_CREATED|")) {
                    // Комната создана успешно
                    String json = payload.substring("ROOM_CREATED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    waitingForPlayer = true;
                    showWaitingPanel();

                } else if (payload.startsWith("PLAYER_JOINED|")) {
                    // Второй игрок присоединился
                    String json = payload.substring("PLAYER_JOINED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    String opponentName = (String) response.get("opponentName");
                    Long opponentId = ((Number) response.get("opponentId")).longValue();

                    waitingForPlayer = false;

                    // Переходим в комнату ожидания
                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("port", roomPort);
                    roomData.put("hostId", currentUserId);
                    roomData.put("opponentId", opponentId);
                    roomData.put("opponentName", opponentName);

                    navigator.navigate(View.WAITING_ROOM, roomData);

                } else if (payload.startsWith("ROOM_ERROR|")) {
                    String error = payload.substring("ROOM_ERROR|".length());
                    showError("Ошибка создания комнаты: " + error);
                    createButton.setDisable(false);

                } else if (payload.startsWith("PORT_IN_USE|")) {
                    showError("Этот порт уже используется");
                    createButton.setDisable(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка обработки ответа сервера");
                createButton.setDisable(false);
            }
        });
    }

    @Override
    public void initData(Object data) {
        generateRandomPort();
        hideWaitingPanel();
        errorLabel.setVisible(false);
        createButton.setDisable(false);
    }
}