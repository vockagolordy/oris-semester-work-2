package ru.itis.scrabble.controllers;

import javafx.fxml.FXML;
import ru.itis.scrabble.navigation.NavigationManager;
import ru.itis.scrabble.network.NetworkClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

/**
 * Базовый класс для всех контроллеров приложения.
 * Обеспечивает доступ к навигации и сетевому клиенту.
 */
public abstract class BaseController {
    protected NavigationManager navigator;
    protected NetworkClient networkService;
    protected ObjectMapper objectMapper = new ObjectMapper();

    // Идентификатор текущего пользователя (устанавливается после авторизации)
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

    // Метод для инициализации данных при переходе
    public void initData(Object data) {
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            if (dataMap.containsKey("userId")) {
                currentUserId = (Long) dataMap.get("userId");
            }
            if (dataMap.containsKey("username")) {
                currentUsername = (String) dataMap.get("username");
            }
        }
    }

    // Метод для отправки сетевых сообщений
    protected void sendNetworkMessage(String type, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String message = type + "|" + payloadJson;
            networkService.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Метод для отправки команд в формате JSON
    protected void sendJsonCommand(String command, Object data) {
        try {
            Map<String, Object> message = Map.of(
                "type", command,
                "payload", data,
                "senderId", currentUserId
            );
            String json = objectMapper.writeValueAsString(message);
            networkService.sendMessage(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Метод, который будет вызываться при получении данных от сокета
    public abstract void handleNetworkMessage(String message);
}