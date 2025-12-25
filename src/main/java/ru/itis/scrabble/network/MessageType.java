package ru.itis.scrabble.network;

public enum MessageType {
    // Core types
    AUTH,
    AUTH_SUCCESS,
    AUTH_ERROR,
    TILE_PREVIEW,
    TURN_COMMIT,
    HEARTBEAT,
    SYNC_STATE,
    GAME_EVENT,
    ERROR,

    // Application-specific actions (used by controllers)
    GET_GAME_STATE,
    MAKE_MOVE,
    SKIP_TURN,
    SURRENDER,
    OFFER_DRAW,
    EXIT_GAME,
    TIME_OUT,
    DRAW_RESPONSE,
    REGISTER,
    FIRST_PLAYER_DETERMINED,
    START_GAME_SESSION,
    LOGOUT,
    GET_CURRENT_STYLE,
    UPDATE_STYLE,
    ROOM_JOIN,
    GET_ROOM_INFO,
    PLAYER_READY,
    START_GAME,
    LEAVE_ROOM,
    RECONNECT,
    GET_ROOMS,
    JOIN_ROOM,
    GET_USER_STATS,
    UPDATE_USERNAME,
    GAME_OVER_CONFIRMED,
    PLAY_AGAIN_REQUEST,
    LEAVE_ROOM_AFTER_GAME,
    CREATE_ROOM,
    CANCEL_WAITING
}

