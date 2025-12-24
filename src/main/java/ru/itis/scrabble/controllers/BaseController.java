package ru.itis.scrabble.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import ru.itis.scrabble.navigation.NavigationManager;
import ru.itis.scrabble.network.NetworkClient;
import ru.itis.scrabble.dto.NetworkMessageDTO; // Убедитесь, что импорт соответствует вашему проекту
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Базовый класс для всех контроллеров приложения.
 * Обеспечивает доступ к навигации и сетевому клиенту.
 */
public abstract class BaseController implements Initializable {
    protected NavigationManager navigator;
    protected NetworkClient networkService;
    protected ObjectMapper objectMapper = new ObjectMapper();

    // Идентификатор текущего пользователя
    protected Long currentUserId;
    protected String currentUsername;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
    }

    public void setNavigator(NavigationManager navigator) {
        this.navigator = navigator;
    }

    public void setNetworkService(NetworkClient networkService) {
        this.networkService = networkService;
        if (networkService != null) {
            networkService.setMessageHandler(this::handleNetworkMessage);
        }
    }

    public void setCurrentUser(Long userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void initData(Object data) {
        if (data instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data;
            if (dataMap.containsKey("userId")) {
                currentUserId = (Long) dataMap.get("userId");
            }
            if (dataMap.containsKey("username")) {
                currentUsername = (String) dataMap.get("username");
            }
        }
    }

    protected void sendNetworkMessage(String type, Object payload) {
        if (networkService == null || !networkService.isConnected()) {
            System.err.println("Ошибка: Сетевая служба не подключена.");
            return;
        }

        try {
            // Превращаем полезную нагрузку в JSON-строку
            String payloadJson = (payload instanceof String)
                    ? (String) payload
                    : objectMapper.writeValueAsString(payload);

            // Отправляем асинхронно чтобы не блокировать JavaFX thread
            networkService.sendMessageAsync(type, payloadJson);

            System.out.println("Отправлена команда: " + type);
        } catch (Exception e) {
            System.err.println("Ошибка при подготовке сообщения: " + e.getMessage());
        }
    }

    protected void sendPlayerAction(String actionType, Object data) {
        sendNetworkMessage(actionType, data);
    }

    /**
     * Convenience helper: serialize `data` to JSON and send as a MessageType command.
     * Uses async send to avoid blocking the JavaFX thread.
     */
    protected void sendJsonCommand(String command, Object data) {
        if (networkService == null || !networkService.isConnected()) {
            System.err.println("Ошибка: Сетевая служба не подключена.");
            return;
        }

        try {
            String payloadJson = (data instanceof String) ? (String) data : objectMapper.writeValueAsString(data);
            networkService.sendMessageAsync(command, payloadJson);
        } catch (Exception e) {
            System.err.println("Ошибка при подготовке JSON-команды: " + e.getMessage());
        }
    }

    /**
     * Default DTO-based handler. For backward compatibility, if a subclass
     * implements `handleNetworkMessage(String)`, this will invoke it with a
     * reconstructed legacy string in the form `TYPE|payload`.
     * Subclasses may override this method to handle `NetworkMessageDTO` directly.
     */
    public void handleNetworkMessage(NetworkMessageDTO message) {
        // Try to call legacy handler if present to avoid touching all controllers at once
        try {
            java.lang.reflect.Method legacy = this.getClass().getMethod("handleNetworkMessage", String.class);
            if (legacy != null) {
                String typePart = message.type() != null ? message.type().name() : "";
                String payloadPart = message.payload() != null ? message.payload() : "";
                String reconstructed = typePart + "|" + payloadPart;
                legacy.invoke(this, reconstructed);
                return;
            }
        } catch (NoSuchMethodException ignored) {
            // No legacy handler, fall through
        } catch (Exception e) {
            System.err.println("Ошибка при вызове legacy handler: " + e.getMessage());
        }

        // If no legacy handler exists and subclass didn't override, nothing to do.
    }
}