package ru.itis.scrabble.dto;

public class AuthResponseDTO {
    private Long userId;
    private String username;
    private int styleId;
    private int totalWins;
    private int totalLoses;
    private int totalGames;

    public AuthResponseDTO(Long userId, String username, int styleId,
                          int totalWins, int totalLoses, int totalGames) {
        this.userId = userId;
        this.username = username;
        this.styleId = styleId;
        this.totalWins = totalWins;
        this.totalLoses = totalLoses;
        this.totalGames = totalGames;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getStyleId() { return styleId; }
    public int getTotalWins() { return totalWins; }
    public int getTotalLoses() { return totalLoses; }
    public int getTotalGames() { return totalGames; }
}