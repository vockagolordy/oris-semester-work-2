package ru.itis.scrabble.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель игровой комнаты.
 * <p>
 * Только данные, без бизнес-логики.
 * Вся логика должна быть в соответствующих сервисах.
 * </p>
 */
public class Room {
    private final int port;
    private Long hostId;
    private final List<Long> users;
    private Game game;

    // Только временные метки для сервисов
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;

    // Флаги состояния (сервисы используют эти флаги)
    private boolean gameStarted;
    private boolean hasActiveDrawOffer;
    private LocalDateTime drawOfferExpiry;

    public Room(int port) {
        this.port = port;
        this.users = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.gameStarted = false;
        this.hasActiveDrawOffer = false;
    }



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

    public List<Long> getUsers() {
        return new ArrayList<>(users);
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
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

    // === Простые операции с коллекциями ===

    public void addUser(Long userId) {
        if (!users.contains(userId)) {
            users.add(userId);
            updateActivity();
        }
    }

    public void removeUser(Long userId) {
        users.remove(userId);
        updateActivity();
    }

    public boolean containsUser(Long userId) {
        return users.contains(userId);
    }

    public boolean isFull() {
        return users.size() >= 2;
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public int getUserCount() {
        return users.size();
    }

    // Вспомогательный метод для сетевого слоя
    public Long getOpponentId(Long userId) {
        if (users.size() != 2) return null;

        for (Long id : users) {
            if (!id.equals(userId)) {
                return id;
            }
        }
        return null;
    }
}