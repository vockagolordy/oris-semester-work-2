package ru.itis.scrabble.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель игровой комнаты.
 * <p>
 * Управляет состоянием лобби и хранит активную игровую сессию (GameSession).
 * </p>
 */
public class Room {
    private final int port;
    private Long hostId;

    private final List<Long> userIds;

    private GameSession gameSession;

    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;

    private boolean gameStarted;
    private boolean hasActiveDrawOffer;
    private LocalDateTime drawOfferExpiry;

    public Room(int port) {
        this.port = port;
        this.userIds = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.gameStarted = false;
        this.hasActiveDrawOffer = false;
    }

    // === Геттеры и сеттеры ===

    public int getPort() {
        return port;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
        updateActivity();
    }

    /**
     * Возвращает ID пользователей, подключенных к комнате.
     */
    public List<Long> getUserIds() {
        return new ArrayList<>(userIds);
    }

    /**
     * Получение текущей активной сессии игры.
     */
    public GameSession getGameSession() {
        return gameSession;
    }

    /**
     * Установка новой игровой сессии (вызывается GameSessionService при старте).
     */
    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
        this.gameStarted = (gameSession != null);
        updateActivity();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
        updateActivity();
    }

    // === Управление состоянием ничьей ===

    public boolean isHasActiveDrawOffer() {
        return hasActiveDrawOffer;
    }

    public void setHasActiveDrawOffer(boolean hasActiveDrawOffer) {
        this.hasActiveDrawOffer = hasActiveDrawOffer;
    }

    public LocalDateTime getDrawOfferExpiry() {
        return drawOfferExpiry;
    }

    public void setDrawOfferExpiry(LocalDateTime drawOfferExpiry) {
        this.drawOfferExpiry = drawOfferExpiry;
    }

    // === Операции с пользователями ===

    public void addUser(Long userId) {
        if (!userIds.contains(userId)) {
            userIds.add(userId);
            if (hostId == null) hostId = userId; // Первый зашедший становится хостом
            updateActivity();
        }
    }

    public void removeUser(Long userId) {
        userIds.remove(userId);
        if (userId.equals(hostId)) {
            hostId = userIds.isEmpty() ? null : userIds.get(0); // Передача хоста
        }
        updateActivity();
    }

    public boolean containsUser(Long userId) {
        return userIds.contains(userId);
    }

    public boolean isFull() {
        return userIds.size() >= 2;
    }

    public boolean isEmpty() {
        return userIds.isEmpty();
    }

    public int getUserCount() {
        return userIds.size();
    }

    /**
     * Поиск оппонента. Используется для уведомлений и preview-пакетов.
     */
    public Long getOpponentId(Long userId) {
        return userIds.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);
    }
}