package ru.itis.scrabble.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * Хранит состояние подключения конкретного клиента.
 */
public class ClientSession {
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final String sessionId;
    private Long userId; // Связь с сущностью User/Player после авторизации

    public ClientSession(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(8192); // 8KB буфер
        this.sessionId = UUID.randomUUID().toString();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}