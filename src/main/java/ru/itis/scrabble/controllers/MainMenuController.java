package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ru.itis.scrabble.navigation.View;

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
        createRoomButton.setOnAction(event -> navigateToCreateRoom());
        joinRoomButton.setOnAction(event -> navigateToRoomList());
        profileButton.setOnAction(event -> navigateToProfile());
        statsButton.setOnAction(event -> navigateToTilesStyles());
        logoutButton.setOnAction(event -> handleLogout());
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
        sendJsonCommand("LOGOUT", Map.of("userId", currentUserId));

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
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            // Обработка сообщений, специфичных для главного меню
            if (message.startsWith("INVITATION|")) {
                // Приглашение в комнату от другого игрока
                String roomInfo = message.substring("INVITATION|".length());
                navigator.showDialog("Приглашение в игру",
                    "Вас приглашают в комнату: " + roomInfo + "\n" +
                    "Перейти в список комнат?");
            }
        });
    }
}