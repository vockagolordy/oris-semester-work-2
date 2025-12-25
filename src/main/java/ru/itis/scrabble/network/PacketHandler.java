package ru.itis.scrabble.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessageDTO;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.services.GameSessionService;

import java.util.List;
import java.util.Map;

/**
 * Диспетчер пакетов. Преобразует JSON в команды для бизнес-логики.
 */
public class PacketHandler {
    private final ObjectMapper objectMapper;
    private final GameSessionService gameSessionService;

    public PacketHandler(GameSessionService gameSessionService) {
        this.objectMapper = new ObjectMapper();
        this.gameSessionService = gameSessionService;
    }

    public void handle(ClientSession session, String json) {
        try {
            NetworkMessageDTO message = objectMapper.readValue(json, NetworkMessageDTO.class);

            switch (message.type()) {
                case AUTH -> handleAuth(session, message);
                case TURN_COMMIT -> handleTurn(session, message);
                case TILE_PREVIEW -> handlePreview(session, message);
                case HEARTBEAT -> gameSessionService.handleHeartbeat(session);
                default -> System.out.println("Unknown message type: " + message.type());
            }
        } catch (JsonProcessingException e) {
            System.err.println("Ошибка парсинга JSON: " + e.getMessage());
            sendError(session, "INVALID_FORMAT", "Некорректный формат сообщения");
        }
    }

    private void handleAuth(ClientSession session, NetworkMessageDTO message) {
        try {
            // Парсим JSON из payload
            Map<String, String> credentials = objectMapper.readValue(message.payload(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});

            String username = credentials.get("username");
            String password = credentials.get("password");

            // Вызов вашего сервиса авторизации
            gameSessionService.authenticate(session, username, password);

            // Получаем ID после успешной аутентификации (предположим, сервис сохранил его в сессию)
            Long userId = session.getUserId();

            if (userId != null) {
                // Формируем JSON-тело ответа
                String userJson = String.format("{\"userId\":%d,\"username\":\"%s\"}", userId, username);

                // Формируем финальную строку ПРЕФИКС|JSON
                String responsePayload = "AUTH_SUCCESS|" + userJson;

                session.sendMessage(new NetworkMessageDTO(MessageType.AUTH, responsePayload, "SERVER"));
            } else {
                session.sendMessage(new NetworkMessageDTO(MessageType.AUTH, "AUTH_ERROR|Неверный логин или пароль", "SERVER"));
            }

        } catch (Exception e) {
            session.sendMessage(new NetworkMessageDTO(MessageType.AUTH, "AUTH_ERROR|Ошибка сервера: " + e.getMessage(), "SERVER"));
        }
    }

    private void handleTurn(ClientSession session, NetworkMessageDTO message) {
        try {
            // Десериализуем список фишек из payload
            List<TilePlacementDTO> placements = objectMapper.readValue(
                    message.payload(),
                    new TypeReference<List<TilePlacementDTO>>() {}
            );
            gameSessionService.commitTurn(session, placements);
        } catch (JsonProcessingException e) {
            sendError(session, "TURN_ERROR", "Ошибка данных хода");
        }
    }

    private void handlePreview(ClientSession session, NetworkMessageDTO message) {
        try {
            List<TilePlacementDTO> placements = objectMapper.readValue(
                    message.payload(),
                    new TypeReference<List<TilePlacementDTO>>() {}
            );
            gameSessionService.processPreview(session, placements);
        } catch (JsonProcessingException e) {
            // Для превью ошибки можно игнорировать или логировать тихо
        }
    }

    private void sendError(ClientSession session, String code, String text) {
        try {
            NetworkMessageDTO error = new NetworkMessageDTO(MessageType.ERROR, text, "SERVER");
            session.sendMessage(error);
        } catch (Exception e) {
            System.err.println("Не удалось отправить ошибку в сессию " + session.getSessionId() + ": " + e.getMessage());
        }
    }
}