package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Room;
import java.util.List;

public interface RoomService {


    Room createRoom(Long hostUserId, int port);

    boolean joinRoom(Long userId, int port);

    boolean leaveRoom(Long userId, int port);

    Room getRoomByPort(int port);

    Room getRoomByUserId(Long userId);

    List<Room> getAllRooms();

    List<Room> getAvailableRooms();

    boolean isRoomFull(int port);

    boolean isGameStarted(int port);

    boolean canReconnect(Long userId, int port);

    void handlePlayerDisconnect(Long userId, int port);

    boolean reconnectPlayer(Long userId, int port);

    void removePlayer(Long userId, int port);

    void updateRoomActivity(int port);

    boolean shouldCleanRoom(int port);

    void cleanupInactiveRooms();

    void cleanupRoom(int port);

    int getActiveRoomCount();

    int getTotalConnectedUsers();

    boolean roomExists(int port);

    boolean isUserInAnyRoom(Long userId);
}