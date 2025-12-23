package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import ru.itis.scrabble.controllers.BaseController;
import ru.itis.scrabble.navigation.NavigationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Централизованный обработчик сообщений от сервера.
 * Распределяет сообщения по соответствующим контроллерам.
 */
public class ServerMessageHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BaseController> activeControllers = new ConcurrentHashMap<>();
    private NavigationManager navigationManager;

    public void setNavigationManager(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    public void registerController(String controllerType, BaseController controller) {
        activeControllers.put(controllerType, controller);
    }

    public void unregisterController(String controllerType) {
        activeControllers.remove(controllerType);
    }

    public void handleMessage(String message) {
        Platform.runLater(() -> {
            try {
                // Парсим сообщение
                if (message.contains("|")) {
                    String[] parts = message.split("\\|", 2);
                    String command = parts[0];
                    String data = parts.length > 1 ? parts[1] : "";

                    // Определяем тип сообщения и передаем соответствующим контроллерам
                    if (command.startsWith("AUTH") || command.startsWith("REGISTER")) {
                        forwardToController("login", message);
                    } else if (command.startsWith("ROOM") || command.startsWith("JOIN")) {
                        forwardToController("room", message);
                    } else if (command.startsWith("GAME") || command.startsWith("MOVE")) {
                        forwardToController("game", message);
                    } else if (command.startsWith("USER") || command.startsWith("STATS")) {
                        forwardToController("profile", message);
                    } else if (command.startsWith("STYLE")) {
                        forwardToController("styles", message);
                    } else {
                        // Глобальные сообщения для всех контроллеров
                        broadcastMessage(message);
                    }
                } else {
                    // Простое текстовое сообщение
                    System.out.println("Server message: " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                navigationManager.showError("Ошибка обработки",
                    "Ошибка при обработке сообщения сервера: " + e.getMessage());
            }
        });
    }

    private void forwardToController(String controllerType, String message) {
        BaseController controller = activeControllers.get(controllerType);
        if (controller != null) {
            controller.handleNetworkMessage(message);
        } else {
            System.err.println("Контроллер не найден для типа: " + controllerType);
        }
    }

    private void broadcastMessage(String message) {
        // Отправляем сообщение всем активным контроллерам
        for (BaseController controller : activeControllers.values()) {
            controller.handleNetworkMessage(message);
        }
    }

    // Методы для регистрации контроллеров из навигатора
    public void setCurrentController(BaseController controller) {
        // Определяем тип контроллера по его классу
        String controllerType = controller.getClass().getSimpleName()
            .replace("Controller", "")
            .toLowerCase();

        registerController(controllerType, controller);
    }
}