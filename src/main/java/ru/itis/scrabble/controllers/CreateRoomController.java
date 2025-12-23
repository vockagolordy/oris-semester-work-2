package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.itis.scrabble.navigation.View;

import java.util.Map;
import java.util.Random;

public class CreateRoomController extends BaseController {

    @FXML private TextField portField;
    @FXML private Button randomPortButton;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    @FXML private VBox waitingPanel;
    @FXML private Label waitingTimerLabel;
    @FXML private Label roomPortLabel;
    @FXML private Button cancelWaitingButton;
    @FXML private Label errorLabel;

    private boolean waitingForPlayer = false;
    private int roomPort;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
        generateRandomPort();
    }

    private void setupEventHandlers() {
        randomPortButton.setOnAction(event -> generateRandomPort());
        createButton.setOnAction(event -> createRoom());
        cancelButton.setOnAction(event -> navigator.navigate(View.MAIN_MENU));
        cancelWaitingButton.setOnAction(event -> cancelWaiting());
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

            sendJsonCommand("CREATE_ROOM", roomData);

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
        sendJsonCommand("CANCEL_WAITING", Map.of("port", roomPort));
        hideWaitingPanel();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                if (message.startsWith("ROOM_CREATED|")) {
                    // Комната создана успешно
                    String json = message.substring("ROOM_CREATED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    waitingForPlayer = true;
                    showWaitingPanel();

                } else if (message.startsWith("PLAYER_JOINED|")) {
                    // Второй игрок присоединился
                    String json = message.substring("PLAYER_JOINED|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    String opponentName = (String) response.get("opponentName");
                    Long opponentId = ((Number) response.get("opponentId")).longValue();

                    waitingForPlayer = false;

                    // Переходим в комнату ожидания
                    Map<String, Object> roomData = Map.of(
                        "port", roomPort,
                        "hostId", currentUserId,
                        "opponentId", opponentId,
                        "opponentName", opponentName
                    );

                    navigator.navigate(View.WAITING_ROOM, roomData);

                } else if (message.startsWith("ROOM_ERROR|")) {
                    String error = message.substring("ROOM_ERROR|".length());
                    showError("Ошибка создания комнаты: " + error);
                    createButton.setDisable(false);

                } else if (message.startsWith("PORT_IN_USE|")) {
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