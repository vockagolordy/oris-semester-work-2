package ru.itis.scrabble.dto;

/**
 * DTO для запроса списка комнат
 */
public class RoomListRequestDTO {
    private Long userId;
    private String username;
    private boolean includeFullRooms; // Включать ли заполненные комнаты

    public RoomListRequestDTO() {}

    public RoomListRequestDTO(Long userId, String username, boolean includeFullRooms) {
        this.userId = userId;
        this.username = username;
        this.includeFullRooms = includeFullRooms;
    }

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isIncludeFullRooms() { return includeFullRooms; }
    public void setIncludeFullRooms(boolean includeFullRooms) { this.includeFullRooms = includeFullRooms; }
}