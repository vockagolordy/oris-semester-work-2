package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import ru.itis.scrabble.navigation.View;

import java.util.Map;

public class ProfileController extends BaseController {

    @FXML private Label usernameLabel;
    @FXML private Label statusLabel;

    @FXML private Label totalGamesLabel;
    @FXML private Label winsLabel;
    @FXML private Label lossesLabel;
    @FXML private Label drawsLabel;
    @FXML private Label winRateLabel;

    @FXML private Label averageScoreLabel;
    @FXML private Label maxScoreLabel;
    @FXML private Label longestWordLabel;
    @FXML private Label roomsCreatedLabel;

    @FXML private Button refreshButton;
    @FXML private Button editProfileButton;
    @FXML private Button backButton;

    private UserStats userStats;

    // Внутренний класс для хранения статистики
    private static class UserStats {
        int totalGames = 0;
        int wins = 0;
        int losses = 0;
        int draws = 0;
        double winRate = 0.0;
        int averageScore = 0;
        int maxScore = 0;
        String longestWord = "-";
        int roomsCreated = 0;
        String status = "В сети";
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        refreshButton.setOnAction(event -> loadProfileData());
        editProfileButton.setOnAction(event -> editProfile());
        backButton.setOnAction(event -> navigator.navigate(View.MAIN_MENU));
    }

    @Override
    public void initData(Object data) {
        userStats = new UserStats();
        loadProfileData();
    }

    private void loadProfileData() {
        // Отправляем запрос на получение статистики пользователя
        sendJsonCommand("GET_USER_STATS", Map.of("userId", currentUserId));

        refreshButton.setDisable(true);
        refreshButton.setText("Загрузка...");
    }

    private void editProfile() {
        // Показываем диалог редактирования профиля
        TextInputDialog dialog = new TextInputDialog(currentUsername);
        dialog.setTitle("Редактирование профиля");
        dialog.setHeaderText("Изменение имени пользователя");
        dialog.setContentText("Новое имя:");

        dialog.showAndWait().ifPresent(newUsername -> {
            if (!newUsername.trim().isEmpty() && !newUsername.equals(currentUsername)) {
                // Отправляем запрос на изменение имени
                Map<String, Object> updateData = Map.of(
                    "userId", currentUserId,
                    "newUsername", newUsername.trim()
                );

                sendJsonCommand("UPDATE_USERNAME", updateData);
            }
        });
    }

    private void updateUI() {
        Platform.runLater(() -> {
            usernameLabel.setText(currentUsername);
            statusLabel.setText(userStats.status);

            totalGamesLabel.setText(String.valueOf(userStats.totalGames));
            winsLabel.setText(String.valueOf(userStats.wins));
            lossesLabel.setText(String.valueOf(userStats.losses));
            drawsLabel.setText(String.valueOf(userStats.draws));
            winRateLabel.setText(String.format("%.1f%%", userStats.winRate));

            averageScoreLabel.setText(String.valueOf(userStats.averageScore));
            maxScoreLabel.setText(String.valueOf(userStats.maxScore));
            longestWordLabel.setText(userStats.longestWord);
            roomsCreatedLabel.setText(String.valueOf(userStats.roomsCreated));

            refreshButton.setDisable(false);
            refreshButton.setText("Обновить");
        });
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                if (message.startsWith("USER_STATS|")) {
                    String json = message.substring("USER_STATS|".length());
                    Map<String, Object> stats = mapper.readValue(json, Map.class);

                    userStats.totalGames = ((Number) stats.getOrDefault("totalGames", 0)).intValue();
                    userStats.wins = ((Number) stats.getOrDefault("wins", 0)).intValue();
                    userStats.losses = ((Number) stats.getOrDefault("losses", 0)).intValue();
                    userStats.draws = ((Number) stats.getOrDefault("draws", 0)).intValue();

                    // Рассчитываем процент побед
                    if (userStats.totalGames > 0) {
                        userStats.winRate = (userStats.wins * 100.0) / userStats.totalGames;
                    }

                    userStats.averageScore = ((Number) stats.getOrDefault("averageScore", 0)).intValue();
                    userStats.maxScore = ((Number) stats.getOrDefault("maxScore", 0)).intValue();
                    userStats.longestWord = (String) stats.getOrDefault("longestWord", "-");
                    userStats.roomsCreated = ((Number) stats.getOrDefault("roomsCreated", 0)).intValue();
                    userStats.status = (String) stats.getOrDefault("status", "В сети");

                    updateUI();

                } else if (message.startsWith("USERNAME_UPDATED|")) {
                    String json = message.substring("USERNAME_UPDATED|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    String newUsername = (String) response.get("username");
                    currentUsername = newUsername;

                    // Обновляем в навигаторе
                    navigator.setCurrentUser(currentUserId, currentUsername);

                    navigator.showDialog("Успех", "Имя пользователя изменено на: " + newUsername);
                    loadProfileData(); // Перезагружаем данные

                } else if (message.startsWith("USERNAME_UPDATE_ERROR|")) {
                    String error = message.substring("USERNAME_UPDATE_ERROR|".length());
                    navigator.showError("Ошибка", "Не удалось изменить имя: " + error);

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);
                    refreshButton.setDisable(false);
                    refreshButton.setText("Обновить");
                }
            } catch (Exception e) {
                e.printStackTrace();
                refreshButton.setDisable(false);
                refreshButton.setText("Обновить");
            }
        });
    }
}