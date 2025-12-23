package ru.itis.scrabble.controllers;

import ru.itis.scrabble.navigation.NavigationManager;
import ru.itis.scrabble.network.NetworkClient;
import ru.itis.scrabble.dto.NetworkMessageDTO; // Убедитесь, что импорт соответствует вашему проекту
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Базовый класс для всех контроллеров приложения.
 * Обеспечивает доступ к навигации и сетевому клиенту.
 */
public abstract class BaseController {
    protected NavigationManager navigator;
    protected NetworkClient networkService;
    protected ObjectMapper objectMapper = new ObjectMapper();

    // Идентификатор текущего пользователя
    protected Long currentUserId;
    protected String currentUsername;

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

            // Отправляем через типизированный метод клиента
            networkService.sendMessage(type, payloadJson);

            System.out.println("Отправлена команда: " + type);
        } catch (Exception e) {
            System.err.println("Ошибка при подготовке сообщения: " + e.getMessage());
        }
    }

    protected void sendPlayerAction(String actionType, Object data) {
        sendNetworkMessage(actionType, data);
    }

    public abstract void handleNetworkMessage(NetworkMessageDTO message);
}