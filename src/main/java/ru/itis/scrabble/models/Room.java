package ru.itis.scrabble.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель игровой комнаты.
 * <p>
 * Представляет виртуальную комнату, где проводиться игра
 * </p>
 *
 * <br>
 * <b>Поля класса:</b>
 * <ul>
 *     <li>{@code port} - сетевой порт, на котором работает комната</li>
 *     <li>{@code users} - список ID пользователей, находящихся в комнате</li>
 * </ul>
 */
public class Room {
    private final int port;

    private final List<Long> users;

    public Room(int port) {
        this.port = port;
        this.users = new ArrayList<>();
    }

    public int getPort() {
        return port;
    }

    public List<Long> getUsers() {
        return users;
    }

    public void addUser(Long userId) {
        this.users.add(userId);
    }

    public void removeUser(Long userId) {
        this.users.remove(userId);
    }
}
