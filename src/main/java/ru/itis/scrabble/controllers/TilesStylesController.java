package ru.itis.scrabble.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import ru.itis.scrabble.navigation.View;

import java.util.Map;

public class TilesStylesController extends BaseController {

    @FXML private StackPane currentStylePreview;
    @FXML private Label currentStyleName;
    @FXML private Label styleDescription;
    @FXML private Label styleEffects;

    @FXML private ToggleGroup stylesToggleGroup;
    @FXML private RadioButton classicRadio;
    @FXML private RadioButton modernRadio;
    @FXML private RadioButton darkRadio;
    @FXML private RadioButton pastelRadio;
    @FXML private RadioButton oceanRadio;
    @FXML private RadioButton autumnRadio;

    @FXML private Button applyButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    private int currentStyleId = 1;
    private Map<Integer, StyleInfo> styles;

    // Информация о стилях
    private static class StyleInfo {
        String name;
        String description;
        String effects;
        String tileColor;
        String tileStroke;
        String textColor;

        StyleInfo(String name, String description, String effects,
                 String tileColor, String tileStroke, String textColor) {
            this.name = name;
            this.description = description;
            this.effects = effects;
            this.tileColor = tileColor;
            this.tileStroke = tileStroke;
            this.textColor = textColor;
        }
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        initializeStyles();
        setupEventHandlers();
        loadCurrentStyle();
    }

    private void initializeStyles() {
        styles = Map.of(
            1, new StyleInfo("Классический",
                "Классический деревянный стиль фишек, напоминающий оригинальную настольную игру.",
                "Влияние на геймплей: только визуальное",
                "#F5DEB3", "#8B4513", "#8B4513"),
            2, new StyleInfo("Современный",
                "Современный зеленый дизайн с чистыми линиями и контрастными цветами.",
                "Влияние на геймплей: только визуальное",
                "#4CAF50", "#2E7D32", "white"),
            3, new StyleInfo("Темный",
                "Темная тема с высоким контрастом для комфортной игры при слабом освещении.",
                "Влияние на геймплей: только визуальное",
                "#2C3E50", "#1A252F", "#ECF0F1"),
            4, new StyleInfo("Пастельный",
                "Нежные пастельные тона для расслабляющей и спокойной игры.",
                "Влияние на геймплей: только визуальное",
                "#E1BEE7", "#9C27B0", "#9C27B0"),
            5, new StyleInfo("Океанский",
                "Морская тема с оттенками синего, напоминающая океанские глубины.",
                "Влияние на геймплей: только визуальное",
                "#2196F3", "#0D47A1", "white"),
            6, new StyleInfo("Осенний",
                "Теплые осенние тона с оранжевыми и желтыми оттенками.",
                "Влияние на геймплей: только визуальное",
                "#FF9800", "#EF6C00", "white")
        );
    }

    private void setupEventHandlers() {
        // Обработчики для радиокнопок
        classicRadio.setOnAction(event -> selectStyle(1));
        modernRadio.setOnAction(event -> selectStyle(2));
        darkRadio.setOnAction(event -> selectStyle(3));
        pastelRadio.setOnAction(event -> selectStyle(4));
        oceanRadio.setOnAction(event -> selectStyle(5));
        autumnRadio.setOnAction(event -> selectStyle(6));

        applyButton.setOnAction(event -> applyStyle());
        resetButton.setOnAction(event -> resetToDefault());
        backButton.setOnAction(event -> navigator.navigate(View.MAIN_MENU));
    }

    private void loadCurrentStyle() {
        // Запрашиваем текущий стиль у сервера
        sendJsonCommand("GET_CURRENT_STYLE", Map.of("userId", currentUserId));
    }

    private void selectStyle(int styleId) {
        StyleInfo style = styles.get(styleId);
        if (style != null) {
            currentStyleId = styleId;
            updateStylePreview(style);
            updateStyleInfo(style);
        }
    }

    private void updateStylePreview(StyleInfo style) {
        // Очищаем preview
        currentStylePreview.getChildren().clear();

        // Создаем превью фишки
        Rectangle tile = new Rectangle(50, 50);
        tile.setArcWidth(8);
        tile.setArcHeight(8);
        tile.setStyle(String.format("-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 2;",
            style.tileColor, style.tileStroke));

        Text letter = new Text("A");
        letter.setStyle(String.format("-fx-font-weight: bold; -fx-font-size: 20; -fx-fill: %s;",
            style.textColor));

        currentStylePreview.getChildren().addAll(tile, letter);
        currentStyleName.setText(style.name);
    }

    private void updateStyleInfo(StyleInfo style) {
        styleDescription.setText(style.description);
        styleEffects.setText(style.effects);
    }

    private void applyStyle() {
        // Отправляем запрос на изменение стиля
        Map<String, Object> styleData = Map.of(
            "userId", currentUserId,
            "styleId", currentStyleId
        );

        sendJsonCommand("UPDATE_STYLE", styleData);

        applyButton.setDisable(true);
        applyButton.setText("Применяется...");
        statusLabel.setText("Применение стиля...");
    }

    private void resetToDefault() {
        // Сбрасываем к стилю по умолчанию (классическому)
        selectStyle(1);
        applyStyle();
    }

    @Override
    public void handleNetworkMessage(String message) {
        Platform.runLater(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                if (message.startsWith("CURRENT_STYLE|")) {
                    String json = message.substring("CURRENT_STYLE|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    int styleId = ((Number) response.get("styleId")).intValue();
                    selectStyle(styleId);

                    // Выбираем соответствующую радиокнопку
                    switch (styleId) {
                        case 1 -> classicRadio.setSelected(true);
                        case 2 -> modernRadio.setSelected(true);
                        case 3 -> darkRadio.setSelected(true);
                        case 4 -> pastelRadio.setSelected(true);
                        case 5 -> oceanRadio.setSelected(true);
                        case 6 -> autumnRadio.setSelected(true);
                    }

                } else if (message.startsWith("STYLE_UPDATED|")) {
                    String json = message.substring("STYLE_UPDATED|".length());
                    Map<String, Object> response = mapper.readValue(json, Map.class);

                    int newStyleId = ((Number) response.get("styleId")).intValue();
                    String styleName = (String) response.get("styleName");

                    navigator.showDialog("Стиль изменен",
                        "Стиль успешно изменен на: " + styleName);

                    applyButton.setDisable(false);
                    applyButton.setText("Применить стиль");
                    statusLabel.setText("Стиль успешно применен");

                    // Обновляем превью
                    selectStyle(newStyleId);

                } else if (message.startsWith("STYLE_UPDATE_ERROR|")) {
                    String error = message.substring("STYLE_UPDATE_ERROR|".length());
                    navigator.showError("Ошибка", "Не удалось изменить стиль: " + error);

                    applyButton.setDisable(false);
                    applyButton.setText("Применить стиль");
                    statusLabel.setText("Ошибка применения стиля");

                } else if (message.startsWith("ERROR|")) {
                    String error = message.substring("ERROR|".length());
                    navigator.showError("Ошибка", error);

                    applyButton.setDisable(false);
                    applyButton.setText("Применить стиль");
                    statusLabel.setText("");
                }
            } catch (Exception e) {
                e.printStackTrace();
                applyButton.setDisable(false);
                applyButton.setText("Применить стиль");
                statusLabel.setText("");
            }
        });
    }
}