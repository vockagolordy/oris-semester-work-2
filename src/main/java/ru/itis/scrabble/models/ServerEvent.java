package ru.itis.scrabble.models;

public enum ServerEvent {
    CHANGE_TURN,
    CHANGE_CELL,
    CHANGE_BOARD,
    UPDATE_SCORES,
    GET_TILES,
    ACCEPT_TURN,
    DENY_TURN,
    TOTAL_SCORES,
    START_GAME,
    FINISH_GAME,
    CONNECTION_ERROR,
    CONNECT,
    DISCONNECT
}
