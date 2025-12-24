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
import ru.itis.scrabble.dto.WaitingRoomInitDTO;
import ru.itis.scrabble.dto.PlayerJoinedDTO;
import ru.itis.scrabble.dto.PlayerReadyChangedDTO;
import ru.itis.scrabble.dto.StartGameRequestDTO;
import ru.itis.scrabble.network.MessageType;

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
        if (data instanceof WaitingRoomInitDTO) {
            WaitingRoomInitDTO dto = (WaitingRoomInitDTO) data;
            roomPort = dto.port();
            hostId = dto.hostId();
            hostName = dto.hostName();
            opponentId = dto.opponentId();
            opponentName = dto.opponentName();
            isHost = hostId != null && hostId.equals(currentUserId);

            updateUI();

            // Notify server we've joined
            Map<String, Object> joinMsg = Map.of(
                "roomPort", roomPort,
                "userId", currentUserId,
                "username", currentUsername
            );
            sendJsonCommand("ROOM_JOIN", joinMsg);

            // Request room info
            sendJsonCommand("GET_ROOM_INFO", Map.of("port", roomPort));

            startTimer();
        } else if (data instanceof Map) {
            // Legacy fallback
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
            Map<String, Object> joinMsg = Map.of(
                "type", "JOIN_ROOM",
                "roomPort", roomPort,
                "userId", currentUserId,
                "username", currentUsername
            );
            sendNetworkMessage("ROOM_JOIN", joinMsg);
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
            "roomPort", roomPort,
            "userId", currentUserId,
            "isReady", isReady
        );

        sendJsonCommand("PLAYER_READY", readyMsg);

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

        StartGameRequestDTO dto = new StartGameRequestDTO(roomPort, hostId);
        sendJsonCommand("START_GAME", dto);

        // Показываем индикатор загрузки
        readyStatusLabel.setText("Начинаем игру...");
        startButton.setDisable(true);
    }

    private void leaveRoom() {
        Map<String, Object> leaveMsg = Map.of(
            "roomPort", roomPort,
            "userId", currentUserId
        );

        sendJsonCommand("LEAVE_ROOM", leaveMsg);

        navigator.navigate(View.MAIN_MENU);
    }

    private void reconnectToGame() {
        Map<String, Object> reconnectMsg = Map.of(
            "roomPort", roomPort,
            "userId", currentUserId
        );

        sendJsonCommand("RECONNECT", reconnectMsg);
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
                String raw = message.payload() != null ? message.payload() : "";
                String prefix;
                String json;
                int sep = raw.indexOf('|');
                if (sep > 0) {
                    prefix = raw.substring(0, sep);
                    json = raw.substring(sep + 1);
                } else {
                    prefix = message.type() != null ? message.type().name() : "";
                    json = raw;
                }

                if ("PLAYER_JOINED".equals(prefix)) {
                    PlayerJoinedDTO dto = objectMapper.readValue(json, PlayerJoinedDTO.class);
                    opponentName = dto.username();
                    opponentId = dto.userId();
                    updateUI();

                } else if ("PLAYER_LEFT".equals(prefix)) {
                    // No payload expected; remove opponent
                    opponentName = null;
                    opponentId = null;
                    updateUI();
                    navigator.showDialog("Игрок вышел", "Другой игрок покинул комнату");

                } else if ("PLAYER_READY_CHANGED".equals(prefix)) {
                    PlayerReadyChangedDTO dto = objectMapper.readValue(json, PlayerReadyChangedDTO.class);
                    Long playerId = dto.userId();
                    boolean playerReady = dto.isReady();
                    if (playerId.equals(opponentId)) {
                        readyStatusLabel.setText(opponentName + " " + (playerReady ? "готов" : "не готов"));
                    }

                } else if ("GAME_STARTING".equals(prefix)) {
                    // Игра начинается, переходим к выбору первого игрока
                    navigator.navigate(View.CHOOSE_FIRST_PLAYER, new ru.itis.scrabble.dto.ChooseFirstPlayerInitDTO(
                        roomPort, hostId, hostName, opponentId, opponentName
                    ));

                } else if ("ROOM_CLOSED".equals(prefix)) {
                    navigator.showError("Комната закрыта", "Хост закрыл комнату");
                    navigator.navigate(View.MAIN_MENU);

                } else if ("RECONNECT_AVAILABLE".equals(prefix)) {
                    // Есть сохраненная игра
                    reconnectPanel.setVisible(true);

                } else if ("ERROR".equals(prefix) || MessageType.ERROR.name().equals(prefix)) {
                    String error = json != null ? json : "";
                    navigator.showError("Ошибка", error);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}