package ru.itis.scrabble.dto;

public record WaitingRoomInitDTO(int port, Long hostId, String hostName, Long opponentId, String opponentName) {
}
