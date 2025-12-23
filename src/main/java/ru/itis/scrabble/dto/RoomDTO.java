package ru.itis.scrabble.dto;

import java.util.List;

/**
 * DTO для передачи информации о комнате
 */
public class RoomDTO {
    private int port;
    private String creator;
    private String status;
    private int playerCount;
    private String hostId;
    private List<String> players;

    public RoomDTO() {}

    public RoomDTO(int port, String creator, String status, int playerCount, String hostId, List<String> players) {
        this.port = port;
        this.creator = creator;
        this.status = status;
        this.playerCount = playerCount;
        this.hostId = hostId;
        this.players = players;
    }

    // Getters and setters
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }

    @Override
    public String toString() {
        return "RoomDTO{" +
               "port=" + port +
               ", creator='" + creator + '\'' +
               ", status='" + status + '\'' +
               ", playerCount=" + playerCount +
               ", hostId='" + hostId + '\'' +
               ", players=" + players +
               '}';
    }
}