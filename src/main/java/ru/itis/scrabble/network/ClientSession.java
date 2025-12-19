// ClientSession.java
package ru.itis.scrabble.network;

import java.nio.channels.SocketChannel;

public class ClientSession {
    private final SocketChannel channel;
    private final int port;
    private Long userId;
    private String username;
    private boolean authenticated;
    private Long currentRoomPort;
    private long lastActivity;

    public ClientSession(SocketChannel channel, int port) {
        this.channel = channel;
        this.port = port;
        this.lastActivity = System.currentTimeMillis();
    }

    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public boolean isInactive(long timeoutMs) {
        return System.currentTimeMillis() - lastActivity > timeoutMs;
    }

    // Геттеры и сеттеры
    public SocketChannel getChannel() { return channel; }
    public int getPort() { return port; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public Long getCurrentRoomPort() { return currentRoomPort; }
    public void setCurrentRoomPort(Long currentRoomPort) { this.currentRoomPort = currentRoomPort; }
}