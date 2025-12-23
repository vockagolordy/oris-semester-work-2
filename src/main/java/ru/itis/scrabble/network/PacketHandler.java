package ru.itis.scrabble.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.services.GameSessionService;

import java.util.List;

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
            NetworkMessage message = objectMapper.readValue(json, NetworkMessage.class);

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

    private void handleAuth(ClientSession session, NetworkMessage message) {
        // Предполагаем, что в payload приходят данные в формате "username:password"
        String[] credentials = message.payload().split(":");
        if (credentials.length == 2) {
            gameSessionService.authenticate(session, credentials[0], credentials[1]);
        } else {
            sendError(session, "AUTH_ERROR", "Неверный формат учетных данных");
        }
    }

    private void handleTurn(ClientSession session, NetworkMessage message) {
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

    private void handlePreview(ClientSession session, NetworkMessage message) {
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
        // Логика отправки ошибки обратно клиенту через NetworkServer
        // Это можно реализовать через callback или напрямую, если есть доступ к серверу
    }
}