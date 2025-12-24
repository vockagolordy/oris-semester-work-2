package ru.itis.scrabble.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.itis.scrabble.controllers.BaseController;
import ru.itis.scrabble.network.NetworkClient;
import ru.itis.scrabble.network.ServerMessageHandler;

import java.io.IOException;

public class NavigationManager {
    private final Stage stage;
    private NetworkClient networkService;
    private ServerMessageHandler messageHandler;
    private Long currentUserId;
    private String currentUsername;

    public NavigationManager(Stage stage) {
        this.stage = stage;
        this.messageHandler = new ServerMessageHandler();
        this.messageHandler.setNavigationManager(this);
    }

    public void setNetworkService(NetworkClient networkService) {
        this.networkService = networkService;
        if (networkService != null) {
            networkService.setMessageHandler(messageHandler::handleMessage);
        }
    }

    public void setCurrentUser(Long userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
    }

    public void navigate(View view) {
        navigate(view, null);
    }

    public void navigate(View view, Object data) {
        try {
            String fxmlPath = "/ru/itis/scrabble/" + view.getFxmlFile();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            BaseController controller = loader.getController();

            // Настраиваем контроллер
            controller.setNavigator(this);
            controller.setNetworkService(networkService);

            if (currentUserId != null && currentUsername != null) {
                controller.setCurrentUser(currentUserId, currentUsername);
            }

            if (data != null) {
                controller.initData(data);
            }

            // Регистрируем контроллер в обработчике сообщений
            messageHandler.setCurrentController(controller);

            // Обновляем сцену
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }

            stage.show();

        } catch (IOException e) {
            System.err.println("Ошибка при загрузке вида: " + view.getFxmlFile());
            e.printStackTrace();
            showError("Ошибка", "Не удалось загрузить интерфейс: " + e.getMessage());
        }
    }

    public void showDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showConfirmation(String title, String message, Runnable onConfirm) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                onConfirm.run();
            }
        });
    }
}