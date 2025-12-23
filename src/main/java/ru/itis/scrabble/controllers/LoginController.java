package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.itis.scrabble.navigation.View;

import java.util.Map;

public class LoginController extends BaseController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        loginButton.setOnAction(event -> handleLogin());
        registerButton.setOnAction(event -> navigateToSignup());

        // Обработка нажатия Enter в полях ввода
        usernameField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Введите имя пользователя и пароль");
            return;
        }

        // Блокируем кнопку на время авторизации
        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        // Отправляем запрос авторизации на сервер
        Map<String, String> authData = Map.of(
            "username", username,
            "password", password
        );

        sendJsonCommand("AUTH", authData);
    }

    private void navigateToSignup() {
        navigator.navigate(View.SIGNUP);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                if (message.startsWith("AUTH_SUCCESS|")) {
                    // Формат: AUTH_SUCCESS|{"userId":123,"username":"name"}
                    String json = message.substring("AUTH_SUCCESS|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    Long userId = ((Number) response.get("userId")).longValue();
                    String username = (String) response.get("username");

                    // Сохраняем данные пользователя в навигаторе
                    navigator.setCurrentUser(userId, username);

                    // Переходим в главное меню
                    navigator.navigate(View.MAIN_MENU);

                } else if (message.startsWith("AUTH_ERROR|")) {
                    String error = message.substring("AUTH_ERROR|".length());
                    showError("Ошибка авторизации: " + error);
                    loginButton.setDisable(false);

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    showError("Ошибка: " + error);
                    loginButton.setDisable(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка обработки ответа сервера");
                loginButton.setDisable(false);
            }
        });
    }

    @Override
    public void initData(Object data) {
        // Очищаем поля при показе
        usernameField.clear();
        passwordField.clear();
        errorLabel.setVisible(false);
        loginButton.setDisable(false);
    }
}