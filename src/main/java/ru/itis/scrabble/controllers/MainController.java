package ru.itis.scrabble.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import ru.itis.scrabble.dto.TilePlacementDTO;
import ru.itis.scrabble.network.NetworkClient;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private GridPane boardGrid; // Сетка 15x15
    @FXML
    private HBox playerRack;    // Подставка для фишек (7 штук)

    private NetworkClient networkClient;
    private final List<TilePlacementDTO> currentTurnPlacements = new ArrayList<>();

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    @FXML
    public void initialize() {
        // TODO: Отрисовать пустую сетку 15x15 с учетом бонусов (DWS, TWS и т.д.)
        // TODO: Настроить Drag-and-Drop для фишек из Rack в GridPane
    }

    @FXML
    private void handleCommitTurn() {
        // TODO: Собрать все TilePlacementDTO из текущего "черновика" на доске
        // TODO: Отправить NetworkMessage с типом TURN_COMMIT через networkClient
    }

    @FXML
    private void handlePassTurn() {
        // TODO: Реализовать пропуск хода
    }

    // Метод вызывается из NetworkClient при получении SYNC_STATE
    public void updateState(String gameStateJson) {
        // TODO: Десериализовать состояние и обновить UI (Platform.runLater)
        // 1. Расставить фишки, которые уже зафиксированы на сервере
        // 2. Обновить очки игроков
        // 3. Обновить фишки в Rack
    }
}