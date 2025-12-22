package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum MessageType {
    AUTH,
    TILE_PREVIEW,
    TURN_COMMIT,
    HEARTBEAT,
    SYNC_STATE,
    GAME_EVENT,
    ERROR
}

