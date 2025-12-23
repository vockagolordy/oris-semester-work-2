package ru.itis.scrabble.dto;

import ru.itis.scrabble.network.MessageType;

public record NetworkMessageDTO(
        MessageType type,
        String payload,
        String senderId
) {

}