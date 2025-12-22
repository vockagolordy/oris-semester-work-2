package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Room;
import java.util.List;

public interface RoomService {


    void createRoom(int port, Long hostId);

    Room getRoom(int port);

    void removeRoom(int port);
}