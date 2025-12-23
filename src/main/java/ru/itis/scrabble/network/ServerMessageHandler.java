package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import ru.itis.scrabble.controllers.BaseController;
import ru.itis.scrabble.navigation.NavigationManager;
import ru.itis.scrabble.dto.NetworkMessageDTO;

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

    public void handleMessage(NetworkMessageDTO message) {
        Platform.runLater(() -> {
            try {
                if (message == null) return;

                MessageType type = message.type();

                switch (type) {
                    case AUTH -> forwardToController("login", message);
                    case TURN_COMMIT, TILE_PREVIEW, SYNC_STATE, GAME_EVENT, HEARTBEAT -> forwardToController("game", message);
                    case ERROR -> navigationManager.showError("Ошибка сервера", message.payload());
                    default -> broadcastMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (navigationManager != null) {
                    navigationManager.showError("Ошибка обработки",
                        "Ошибка при обработке сообщения сервера: " + e.getMessage());
                }
            }
        });
    }

    private void forwardToController(String controllerType, NetworkMessageDTO message) {
        BaseController controller = activeControllers.get(controllerType);
        if (controller != null) {
            controller.handleNetworkMessage(message);
        } else {
            System.err.println("Контроллер не найден для типа: " + controllerType);
        }
    }

    private void broadcastMessage(NetworkMessageDTO message) {
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