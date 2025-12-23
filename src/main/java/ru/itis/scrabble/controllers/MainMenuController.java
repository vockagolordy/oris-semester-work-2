package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ru.itis.scrabble.navigation.View;
import ru.itis.scrabble.dto.NetworkMessage;

import java.util.Map;

public class MainMenuController extends BaseController {

    @FXML private Button createRoomButton;
    @FXML private Button joinRoomButton;
    @FXML private Button profileButton;
    @FXML private Button statsButton;
    @FXML private Button logoutButton;
    @FXML private Label userInfoLabel;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        createRoomButton.setOnAction(_ -> navigateToCreateRoom());
        joinRoomButton.setOnAction(_ -> navigateToRoomList());
        profileButton.setOnAction(_ -> navigateToProfile());
        statsButton.setOnAction(_ -> navigateToTilesStyles());
        logoutButton.setOnAction(_ -> handleLogout());
    }

    private void navigateToCreateRoom() {
        navigator.navigate(View.CREATE_ROOM);
    }

    private void navigateToRoomList() {
        navigator.navigate(View.ROOM_LIST);
    }

    private void navigateToProfile() {
        navigator.navigate(View.PROFILE);
    }

    private void navigateToTilesStyles() {
        navigator.navigate(View.TILES_STYLES);
    }

    private void handleLogout() {
        // Отправляем команду выхода
        sendNetworkMessage("LOGOUT", Map.of("userId", currentUserId));

        // Сбрасываем данные пользователя
        navigator.setCurrentUser(null, null);
        currentUserId = null;
        currentUsername = null;

        // Переходим на экран входа
        navigator.navigate(View.LOGIN);
    }

    @Override
    public void initData(Object data) {
        // Обновляем информацию о пользователе
        if (currentUsername != null) {
            userInfoLabel.setText("Пользователь: " + currentUsername);
        }
    }

    @Override
    public void handleNetworkMessage(NetworkMessage message) {
        Platform.runLater(() -> {
            // Обработка сообщений, специфичных для главного меню
            String payload = message.payload();
            if (payload.startsWith("INVITATION|")) {
                // Приглашение в комнату от другого игрока
                String roomInfo = payload.substring("INVITATION|".length());
                navigator.showDialog("Приглашение в игру",
                    "Вас приглашают в комнату: " + roomInfo + "\n" +
                    "Перейти в список комнат?");
            }
        });
    }
}