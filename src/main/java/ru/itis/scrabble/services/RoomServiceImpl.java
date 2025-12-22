package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Room;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomServiceImpl implements RoomService {

    private final Map<Integer, Room> activeRooms = new ConcurrentHashMap<>();

    @Override
    public void createRoom(int port, Long hostId) {
        activeRooms.put(port, new Room(port));
    }

    @Override
    public Room getRoom(int port) {
        return activeRooms.get(port);
    }

    @Override
    public void removeRoom(int port) {
        activeRooms.remove(port);
    }
}