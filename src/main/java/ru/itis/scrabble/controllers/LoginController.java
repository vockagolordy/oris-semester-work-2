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

                if ("AUTH_SUCCESS".equals(prefix)) {
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);
                    Long userId = ((Number) response.get("userId")).longValue();
                    String username = (String) response.get("username");
                    navigator.setCurrentUser(userId, username);
                    navigator.navigate(View.MAIN_MENU);

                } else if ("AUTH_ERROR".equals(prefix)) {
                    String error = json != null ? json : "";
                    showError("Ошибка авторизации: " + error);
                    loginButton.setDisable(false);

                } else if ("ERROR".equals(prefix) || MessageType.ERROR.name().equals(prefix)) {
                    String error = json != null ? json : "";
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