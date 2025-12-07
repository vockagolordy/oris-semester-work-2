package ru.itis.scrabble.models;

import java.time.LocalDateTime;


/**
 * Модель сессии пользователя.
 * <p>
 * Представляет активную сессию аутентифицированного пользователя.
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code sessionId} - уникальный идентификатор сессии</li>
 *     <li>{@code userId} - ID пользователя, которому принадлежит сессия</li>
 *     <li>{@code expiresAt} - дата и время истечения срока действия сессии</li>
 * </ul>
 */
public class Session {
    private final String sessionId;

    private final Long userId;

    private final LocalDateTime expiresAt;

    public Session(String sessionId, Long userId, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
