package ru.itis.scrabble.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ru.itis.scrabble.dto.NetworkMessageDTO;
import ru.itis.scrabble.navigation.View;

import java.util.Map;

public class SignupController extends BaseController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField passwordField1;
    @FXML private Button registerButton;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        registerButton.setOnAction(_ -> handleRegistration());
        loginButton.setOnAction(_ -> navigateToLogin());

        // Обработка нажатия Enter
        usernameField.setOnAction(_ -> handleRegistration());
        passwordField.setOnAction(_ -> handleRegistration());
        passwordField1.setOnAction(_ -> handleRegistration());
    }

    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = passwordField1.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают");
            return;
        }

        if (password.length() < 4) {
            showError("Пароль должен содержать минимум 4 символа");
            return;
        }

        if (username.length() < 3) {
            showError("Имя пользователя должно содержать минимум 3 символа");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setVisible(false);

        // Отправляем запрос регистрации
        Map<String, String> regData = Map.of(
            "username", username,
            "password", password
        );

        sendNetworkMessage("REGISTER", regData);
    }

    private void navigateToLogin() {
        navigator.navigate(View.LOGIN);
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
                if (payload.startsWith("REGISTER_SUCCESS|")) {
                    // Формат: REGISTER_SUCCESS|{"userId":123,"username":"name"}
                    String json = payload.substring("REGISTER_SUCCESS|".length());
                    Map<String, Object> response = objectMapper.readValue(json, Map.class);

                    Long userId = ((Number) response.get("userId")).longValue();
                    String username = (String) response.get("username");

                    // Автоматически логинимся после регистрации
                    navigator.setCurrentUser(userId, username);
                    navigator.navigate(View.MAIN_MENU);

                } else if (payload.startsWith("REGISTER_ERROR|")) {
                    String error = payload.substring("REGISTER_ERROR|".length());
                    showError("Ошибка регистрации: " + error);
                    registerButton.setDisable(false);

                } else if (payload.startsWith("ERROR|")) {
                    String error = payload.substring("ERROR|".length());
                    showError("Ошибка: " + error);
                    registerButton.setDisable(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка обработки ответа сервера");
                registerButton.setDisable(false);
            }
        });
    }

    @Override
    public void initData(Object data) {
        usernameField.clear();
        passwordField.clear();
        passwordField1.clear();
        errorLabel.setVisible(false);
        registerButton.setDisable(false);
    }
}