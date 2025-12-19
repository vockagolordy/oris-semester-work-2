package ru.itis.scrabble.services;

import ru.itis.scrabble.models.Room;
import ru.itis.scrabble.models.Game;
import ru.itis.scrabble.models.Player;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomServiceImpl implements RoomService {
    // port -> Room
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();
    // userId -> port
    private final Map<Long, Integer> userRooms = new ConcurrentHashMap<>();

    // Для хранения состояния при дисконнекте: userId -> {port, timestamp, gameState}
    private final Map<Long, DisconnectedPlayer> disconnectedPlayers = new ConcurrentHashMap<>();

    // Время для очистки комнат (в минутах)
    private static final int EMPTY_ROOM_CLEANUP_MINUTES = 5;
    private static final int INACTIVE_ROOM_CLEANUP_MINUTES = 10;
    private static final int RECONNECT_TIMEOUT_MINUTES = 1;

    @Override
    public Room createRoom(Long hostUserId, int port) {
        if (rooms.containsKey(port)) {
            System.out.println("Комната на порту " + port + " уже существует");
            return rooms.get(port);
        }

        Room room = new Room(port);
        room.setHostId(hostUserId);
        room.addUser(hostUserId);
        rooms.put(port, room);

        userRooms.put(hostUserId, port);

        System.out.println("Создана комната на порту " + port + ", хост: " + hostUserId);
        return room;
    }

    @Override
    public boolean joinRoom(Long userId, int port) {
        Room room = rooms.get(port);
        if (room == null) {
            System.out.println("Комната на порту " + port + " не найдена");
            return false;
        }

        if (room.isFull()) {
            System.out.println("Комната на порту " + port + " уже заполнена");
            return false;
        }

        if (room.isGameStarted() && !room.containsUser(userId)) {
            System.out.println("Игра уже начата, нельзя присоединиться новому игроку");
            return false;
        }

        // Проверяем, не был ли игрок отключен
        DisconnectedPlayer disconnected = disconnectedPlayers.get(userId);
        if (disconnected != null && disconnected.port == port) {
            // Восстанавливаем из отключенных
            return reconnectPlayer(userId, port);
        }

        room.addUser(userId);
        userRooms.put(userId, port);

        // Обновляем активность
        room.updateActivity();

        System.out.println("Игрок " + userId + " присоединился к комнате на порту " + port);
        return true;
    }

    @Override
    public boolean leaveRoom(Long userId, int port) {
        Room room = rooms.get(port);
        if (room == null) {
            return false;
        }

        room.removeUser(userId);
        userRooms.remove(userId);

        // Если комната пустая, помечаем для очистки
        if (room.isEmpty()) {
            System.out.println("Комната на порту " + port + " пустая, будет очищена через " +
                    EMPTY_ROOM_CLEANUP_MINUTES + " минут");
        } else if (room.getHostId().equals(userId)) {
            // Если вышел хост, назначаем нового
            assignNewHost(room);
        }

        room.updateActivity();
        System.out.println("Игрок " + userId + " покинул комнату на порту " + port);
        return true;
    }

    @Override
    public Room getRoomByPort(int port) {
        return rooms.get(port);
    }

    @Override
    public Room getRoomByUserId(Long userId) {
        Integer port = userRooms.get(userId);
        return port != null ? rooms.get(port) : null;
    }

    @Override
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    @Override
    public List<Room> getAvailableRooms() {
        List<Room> available = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.isGameStarted() && !room.isFull()) {
                available.add(room);
            }
        }
        return available;
    }

    @Override
    public boolean isRoomFull(int port) {
        Room room = rooms.get(port);
        return room != null && room.isFull();
    }

    @Override
    public boolean isGameStarted(int port) {
        Room room = rooms.get(port);
        return room != null && room.isGameStarted();
    }

    @Override
    public boolean canReconnect(Long userId, int port) {
        DisconnectedPlayer disconnected = disconnectedPlayers.get(userId);
        if (disconnected == null || disconnected.port != port) {
            return false;
        }

        // Проверяем, не истекло ли время на переподключение
        long minutesSinceDisconnect = ChronoUnit.MINUTES.between(
                disconnected.disconnectTime, LocalDateTime.now()
        );

        return minutesSinceDisconnect <= RECONNECT_TIMEOUT_MINUTES;
    }

    @Override
    public void handlePlayerDisconnect(Long userId, int port) {
        Room room = rooms.get(port);
        if (room == null) {
            return;
        }

        // Сохраняем состояние для переподключения
        if (room.containsUser(userId)) {
            // Удаляем из активных игроков
            room.removeUser(userId);

            // Сохраняем информацию об отключении
            String gameState = null;
            if (room.isGameStarted() && room.getGame() != null) {
                // Сериализуем состояние игры (в реальности нужно реализовать)
                gameState = serializeGameState(room);
            }

            DisconnectedPlayer disconnected = new DisconnectedPlayer(
                    userId, port, LocalDateTime.now(), gameState
            );
            disconnectedPlayers.put(userId, disconnected);

            System.out.println("Игрок " + userId + " отключился от комнаты " + port +
                    ", сохранено состояние для переподключения");

            // Останавливаем игру, если она была начата
            if (room.isGameStarted()) {
                room.setGameStarted(false);
                System.out.println("Игра приостановлена в комнате " + port);
            }

            // Уведомляем оставшегося игрока
            Long opponentId = room.getOpponentId(userId);
            if (opponentId != null) {
                System.out.println("Уведомлен оппонент " + opponentId + " об отключении игрока " + userId);
            }
        }
    }

    @Override
    public boolean reconnectPlayer(Long userId, int port) {
        DisconnectedPlayer disconnected = disconnectedPlayers.get(userId);
        if (disconnected == null || disconnected.port != port) {
            return false;
        }

        Room room = rooms.get(port);
        if (room == null) {
            return false;
        }

        // Проверяем время
        if (!canReconnect(userId, port)) {
            disconnectedPlayers.remove(userId);
            return false;
        }

        // Восстанавливаем игрока
        room.addUser(userId);

        // Восстанавливаем состояние игры, если было
        if (disconnected.gameState != null) {
            // Десериализуем состояние (в реальности нужно реализовать)
            // room.setGame(deserializeGameState(disconnected.gameState));
            room.setGameStarted(true);
        }

        // Удаляем из отключенных
        disconnectedPlayers.remove(userId);

        room.updateActivity();
        System.out.println("Игрок " + userId + " переподключился к комнате " + port);
        return true;
    }

    @Override
    public void removePlayer(Long userId, int port) {
        // Удаляем полностью (без возможности переподключения)
        Room room = rooms.get(port);
        if (room != null) {
            room.removeUser(userId);
            disconnectedPlayers.remove(userId);
            userRooms.remove(userId);

            if (room.isEmpty()) {
                System.out.println("Комната " + port + " пустая после удаления игрока " + userId);
            }
        }
    }

    @Override
    public void updateRoomActivity(int port) {
        Room room = rooms.get(port);
        if (room != null) {
            room.updateActivity();
        }
    }

    @Override
    public boolean shouldCleanRoom(int port) {
        Room room = rooms.get(port);
        if (room == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Проверяем пустую комнату
        if (room.isEmpty()) {
            long minutesEmpty = ChronoUnit.MINUTES.between(room.getLastActivity(), now);
            return minutesEmpty >= EMPTY_ROOM_CLEANUP_MINUTES;
        }

        // Проверяем неактивную комнату
        long minutesInactive = ChronoUnit.MINUTES.between(room.getLastActivity(), now);
        return minutesInactive >= INACTIVE_ROOM_CLEANUP_MINUTES;
    }

    @Override
    public void cleanupInactiveRooms() {
        List<Integer> portsToRemove = new ArrayList<>();

        for (Map.Entry<Integer, Room> entry : rooms.entrySet()) {
            int port = entry.getKey();
            Room room = entry.getValue();

            if (shouldCleanRoom(port)) {
                portsToRemove.add(port);
                System.out.println("Планируется очистка комнаты на порту " + port);
            }
        }

        for (int port : portsToRemove) {
            cleanupRoom(port);
        }
    }

    @Override
    public void cleanupRoom(int port) {
        Room room = rooms.remove(port);
        if (room != null) {
            // Удаляем всех пользователей из маппингов
            for (Long userId : room.getUsers()) {
                userRooms.remove(userId);
                disconnectedPlayers.remove(userId);
            }

            System.out.println("Комната на порту " + port + " очищена");
        }
    }

    @Override
    public int getActiveRoomCount() {
        int count = 0;
        for (Room room : rooms.values()) {
            if (!room.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTotalConnectedUsers() {
        return userRooms.size();
    }

    @Override
    public boolean roomExists(int port) {
        return rooms.containsKey(port);
    }

    @Override
    public boolean isUserInAnyRoom(Long userId) {
        return userRooms.containsKey(userId);
    }

    private void assignNewHost(Room room) {
        if (room == null || room.isEmpty()) {
            return;
        }

        List<Long> users = room.getUsers();
        if (!users.isEmpty()) {
            Long newHost = users.get(0);
            room.setHostId(newHost);
            System.out.println("Новый хост комнаты " + room.getPort() + ": " + newHost);
        }
    }

    private String serializeGameState(Room room) {
        // TODO
        // В реальности нужно сериализовать все состояние игры
        if (room.getGame() == null) {
            return "NO_GAME";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PORT:").append(room.getPort()).append(";");
        sb.append("STARTED:").append(room.isGameStarted()).append(";");
        sb.append("PLAYERS:").append(room.getUsers().size());

        return sb.toString();
    }

    // Внутренний класс для хранения информации об отключенных игроках
    private static class DisconnectedPlayer {
        final Long userId;
        final int port;
        final LocalDateTime disconnectTime;
        final String gameState;

        DisconnectedPlayer(Long userId, int port, LocalDateTime disconnectTime, String gameState) {
            this.userId = userId;
            this.port = port;
            this.disconnectTime = disconnectTime;
            this.gameState = gameState;
        }
    }
}