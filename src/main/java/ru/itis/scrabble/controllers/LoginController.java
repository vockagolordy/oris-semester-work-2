package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.itis.scrabble.navigation.View;
import ru.itis.scrabble.dto.NetworkMessageDTO;
import ru.itis.scrabble.network.MessageType;

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
        loginButton.setOnAction(_ -> handleLogin());
        registerButton.setOnAction(_ -> navigateToSignup());

        // Обработка нажатия Enter в полях ввода
        usernameField.setOnAction(_ -> handleLogin());
        passwordField.setOnAction(_ -> handleLogin());
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

        sendNetworkMessage("AUTH", authData);
    }

    private void navigateToSignup() {
        navigator.navigate(View.SIGNUP);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @Override
    public void handleNetworkMessage(NetworkMessageDTO message) {
        Platform.runLater(() -> {
            try {
                // Теперь проверяем через Enum MessageType
                if (message.type() == MessageType.AUTH_SUCCESS) {
                    // Извлекаем JSON из payload (теперь это строка)
                    String json = message.payload();
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    // Безопасное извлечение ID
                    Long userId = ((Number) response.get("userId")).longValue();
                    String username = (String) response.get("username");

                    navigator.setCurrentUser(userId, username);
                    navigator.navigate(View.MAIN_MENU);

                } else if (message.type() == MessageType.AUTH_ERROR) {
                    showError("Ошибка: " + message.payload());
                    loginButton.setDisable(false);

                } else if (message.type() == MessageType.ERROR) {
                    showError("Системная ошибка: " + message.payload());
                    loginButton.setDisable(false);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка обработки данных: " + e.getMessage());
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