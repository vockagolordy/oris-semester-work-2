package ru.itis.scrabble.dto;

public record ChooseFirstPlayerInitDTO(int roomPort, Long hostId, String hostName, Long opponentId, String opponentName) {
}
