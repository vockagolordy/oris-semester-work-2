package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.itis.scrabble.navigation.View;
import ru.itis.scrabble.dto.NetworkMessageDTO;
import ru.itis.scrabble.network.MessageType;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RoomListController extends BaseController {

    @FXML private TableView<RoomInfo> roomsTable;
    @FXML private TableColumn<RoomInfo, Integer> portColumn;
    @FXML private TableColumn<RoomInfo, String> creatorColumn;
    @FXML private TableColumn<RoomInfo, String> statusColumn;
    @FXML private TableColumn<RoomInfo, Integer> playerCountColumn;

    @FXML private Button refreshButton;
    @FXML private Button joinButton;
    @FXML private Button backButton;

    @FXML private TextField manualPortField;
    @FXML private Button connectButton;

    @FXML private Label errorLabel;

    private ObservableList<RoomInfo> roomList = FXCollections.observableArrayList();

    // Модель данных для таблицы комнат
    public static class RoomInfo {
        private final SimpleIntegerProperty port;
        private final SimpleStringProperty creator;
        private final SimpleStringProperty status;
        private final SimpleIntegerProperty playerCount;
        private final String hostId; // Добавляем для идентификации хоста

        public RoomInfo(int port, String creator, String status, int playerCount, String hostId) {
            this.port = new SimpleIntegerProperty(port);
            this.creator = new SimpleStringProperty(creator);
            this.status = new SimpleStringProperty(status);
            this.playerCount = new SimpleIntegerProperty(playerCount);
            this.hostId = hostId;
        }

        public int getPort() { return port.get(); }
        public String getCreator() { return creator.get(); }
        public String getStatus() { return status.get(); }
        public int getPlayerCount() { return playerCount.get(); }
        public String getHostId() { return hostId; }
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        setupTable();
        setupEventHandlers();
        loadRoomList();
    }

    private void setupTable() {
        // Настраиваем привязку данных к колонкам
        portColumn.setCellValueFactory(cellData -> cellData.getValue().port.asObject());
        creatorColumn.setCellValueFactory(cellData -> cellData.getValue().creator);
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().status);
        playerCountColumn.setCellValueFactory(cellData -> cellData.getValue().playerCount.asObject());

        roomsTable.setItems(roomList);

        // Настраиваем двойной клик для входа в комнату
        roomsTable.setRowFactory(tv -> {
            TableRow<RoomInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    joinSelectedRoom();
                }
            });
            return row;
        });

        // Настраиваем сортировку по умолчанию (по порту)
        roomsTable.getSortOrder().add(portColumn);
    }

    private void setupEventHandlers() {
        refreshButton.setOnAction(_ -> loadRoomList());
        joinButton.setOnAction(_ -> joinSelectedRoom());
        backButton.setOnAction(_ -> navigator.navigate(View.MAIN_MENU));
        connectButton.setOnAction(_ -> connectToManualPort());

        // Обработка нажатия Enter в поле ввода порта
        manualPortField.setOnAction(_ -> connectToManualPort());
    }

    private void loadRoomList() {
        // Отправляем запрос на получение списка комнат
        sendNetworkMessage("GET_ROOMS", Map.of("userId", currentUserId, "username", currentUsername));

        // Временно блокируем кнопки
        refreshButton.setDisable(true);
        refreshButton.setText("Обновление...");
        errorLabel.setVisible(false);
    }

    private void joinSelectedRoom() {
        RoomInfo selected = roomsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            joinRoom(selected.getPort());
        } else {
            showError("Выберите комнату из списка");
        }
    }

    private void connectToManualPort() {
        String portText = manualPortField.getText().trim();
        if (portText.isEmpty()) {
            showError("Введите номер порта");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                showError("Порт должен быть в диапазоне 1024-65535");
                return;
            }
            joinRoom(port);
        } catch (NumberFormatException e) {
            showError("Введите корректный номер порта");
        }
    }

    private void joinRoom(int port) {
        // Блокируем кнопки на время подключения
        joinButton.setDisable(true);
        connectButton.setDisable(true);
        errorLabel.setVisible(false);

        // Отправляем запрос на присоединение к комнате
        Map<String, Object> joinData = Map.of(
            "type", "JOIN_ROOM",
            "port", port,
            "userId", currentUserId,
            "username", currentUsername
        );

        sendNetworkMessage("JOIN_ROOM", joinData);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }

    @Override
    public void handleNetworkMessage(NetworkMessageDTO message) {
        Platform.runLater(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String raw = message.payload() != null ? message.payload() : "";
                String prefix;
                String json;
                int sep = raw.indexOf('|');
                if (sep > 0) {
                    prefix = raw.substring(0, sep);
                    json = raw.substring(sep + 1);
                } else {
                    prefix = message.type() != null ? message.type().name() : "";
                    json = raw;
                }

                if ("ROOM_LIST".equals(prefix)) {
                    // Получен список комнат
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rooms = (List<Map<String, Object>>) response.get("rooms");

                    // Обновляем список комнат
                    roomList.clear();
                    if (rooms != null) {
                        for (Map<String, Object> room : rooms) {
                            int port = ((Number) room.get("port")).intValue();
                            String creator = (String) room.get("creator");
                            String status = (String) room.get("status");
                            int playerCount = ((Number) room.get("playerCount")).intValue();
                            String hostId = (String) room.get("hostId");

                            roomList.add(new RoomInfo(port, creator, status, playerCount, hostId));
                        }
                    }

                    // Восстанавливаем кнопки
                    refreshButton.setDisable(false);
                    refreshButton.setText("Обновить");
                    roomsTable.sort();

                    // Показываем статус
                    if (roomList.isEmpty()) {
                        showError("Нет доступных комнат. Создайте свою!");
                    } else {
                        clearError();
                    }

                } else if ("JOIN_SUCCESS".equals(prefix)) {
                    // Успешно присоединились к комнате
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    int port = ((Number) response.get("port")).intValue();
                    String hostId = (String) response.get("hostId");
                    String hostName = (String) response.get("hostName");
                    String opponentName = (String) response.get("opponentName");
                    Long opponentId = response.containsKey("opponentId") ?
                        ((Number) response.get("opponentId")).longValue() : null;

                    // Восстанавливаем кнопки
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                    // Переходим в комнату ожидания
                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("port", port);
                    roomData.put("hostId", hostId);
                    roomData.put("hostName", hostName);
                    roomData.put("isHost", hostId.equals(String.valueOf(currentUserId)));

                    // Если есть информация об оппоненте
                    if (opponentName != null && opponentId != null) {
                        roomData.put("opponentId", opponentId);
                        roomData.put("opponentName", opponentName);
                    }

                    navigator.navigate(View.WAITING_ROOM, roomData);

                } else if ("JOIN_ERROR".equals(prefix)) {
                    // Ошибка при присоединении
                    String error = json != null ? json : "";
                    showError("Ошибка подключения: " + error);

                    // Восстанавливаем кнопки
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ROOM_FULL".equals(prefix)) {
                    showError("Комната уже заполнена (максимум 2 игрока)");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ROOM_NOT_FOUND".equals(prefix)) {
                    showError("Комната не найдена или уже закрыта");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ROOM_CLOSED".equals(prefix)) {
                    showError("Комната была закрыта создателем");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ALREADY_IN_ROOM".equals(prefix)) {
                    showError("Вы уже находитесь в комнате");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("GAME_IN_PROGRESS".equals(prefix)) {
                    showError("В этой комнате уже идет игра");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("INVALID_ROOM".equals(prefix)) {
                    showError("Некорректная комната");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ERROR".equals(prefix) || MessageType.ERROR.name().equals(prefix)) {
                    // Общая ошибка
                    String error = json != null ? json : "";
                    showError("Ошибка: " + error);

                    // Восстанавливаем кнопки
                    refreshButton.setDisable(false);
                    refreshButton.setText("Обновить");
                    joinButton.setDisable(false);
                    connectButton.setDisable(false);

                } else if ("ROOM_CREATED_BROADCAST".equals(prefix)) {
                    // Кто-то создал новую комнату - обновляем список
                    loadRoomList();

                } else if ("ROOM_CLOSED_BROADCAST".equals(prefix)) {
                    // Кто-то закрыл комнату - обновляем список
                    loadRoomList();

                } else if ("NEW_ROOM_AVAILABLE".equals(prefix)) {
                    // Новая комната доступна - показываем уведомление
                    String roomInfo = json != null ? json : "";
                    navigator.showDialog("Новая комната",
                        "Появилась новая комната: " + roomInfo + "\n" +
                        "Обновите список, чтобы увидеть её.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError("Ошибка обработки данных от сервера");

                // Восстанавливаем кнопки при любой ошибке
                refreshButton.setDisable(false);
                refreshButton.setText("Обновить");
                joinButton.setDisable(false);
                connectButton.setDisable(false);
            }
        });
    }

    @Override
    public void initData(Object data) {
        // При показе экрана обновляем список комнат
        loadRoomList();
        clearError();
        manualPortField.clear();

        // Восстанавливаем состояние кнопок
        refreshButton.setDisable(false);
        joinButton.setDisable(false);
        connectButton.setDisable(false);
    }

    // Дополнительные методы для работы с UI

    /**
     * Автоматическое обновление списка комнат
     */
    public void startAutoRefresh() {
        // Можно реализовать автоматическое обновление каждые 10 секунд
        // Например, с помощью Timeline или ScheduledExecutorService
    }

    /**
     * Остановка автоматического обновления
     */
    public void stopAutoRefresh() {
        // Остановить таймер или scheduled task
    }

    /**
     * Фильтрация комнат по статусу
     */
    public void filterRooms(String filter) {
        // Реализация фильтрации, если потребуется
    }
}