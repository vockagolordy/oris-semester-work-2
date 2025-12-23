package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class ClientSession {
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final String sessionId;
    private Long userId;
    private String username;
    private int roomId = -1; // -1 означает, что игрок еще не в комнате

    private static final ObjectMapper mapper = new ObjectMapper();

    public ClientSession(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(16384);
        this.sessionId = UUID.randomUUID().toString();
    }

    /**
     * Отправка сообщения по протоколу [4 байта длины] + [JSON]
     */
    public void sendMessage(NetworkMessage msg) {
        try {
            byte[] body = mapper.writeValueAsBytes(msg);
            ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
            buffer.putInt(body.length);
            buffer.put(body);
            buffer.flip();

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения сессии " + sessionId + ": " + e.getMessage());
        }
    }

    // Геттеры и сеттеры
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public ByteBuffer getReadBuffer() { return readBuffer; }
    public String getSessionId() { return sessionId; }
}