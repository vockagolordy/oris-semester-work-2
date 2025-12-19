package ru.itis.scrabble.network;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final Map<Long, SocketChannel> userConnections; // userId → channel
    private final Map<SocketChannel, Long> channelToUser; // channel → userId

    public ConnectionManager() {
        this.userConnections = new ConcurrentHashMap<>();
        this.channelToUser = new ConcurrentHashMap<>();
    }

    public void registerConnection(Long userId, SocketChannel channel) {
        if (userId != null && channel != null) {
            userConnections.put(userId, channel);
            channelToUser.put(channel, userId);
        }
    }

    public void removeConnection(SocketChannel channel) {
        Long userId = channelToUser.remove(channel);
        if (userId != null) {
            userConnections.remove(userId);
        }
    }

    public SocketChannel getConnection(Long userId) {
        return userConnections.get(userId);
    }

    public Long getUserId(SocketChannel channel) {
        return channelToUser.get(channel);
    }

    public boolean isUserConnected(Long userId) {
        SocketChannel channel = userConnections.get(userId);
        return channel != null && channel.isConnected();
    }

    public int getConnectedUserCount() {
        return userConnections.size();
    }
}