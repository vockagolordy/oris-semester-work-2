package ru.itis.scrabble.navigation;

public enum View {
    LOGIN("login-view.fxml"),
    SIGNUP("signup-view.fxml"),
    MAIN_MENU("main-menu-view.fxml"),
    PROFILE("profile-view.fxml"),
    ROOM_LIST("room-list-view.fxml"),
    CREATE_ROOM("create-room-view.fxml"),
    WAITING_ROOM("waiting-room-view.fxml"),
    CHOOSE_FIRST_PLAYER("choose-first-player-view.fxml"),
    GAME("game-view.fxml"),
    GAME_OVER("game-over-view.fxml"),
    TILES_STYLES("tiles-styles-view.fxml");

    private final String fxmlFile;

    View(String fxmlFile) {
        this.fxmlFile = fxmlFile;
    }

    public String getFxmlFile() {
        return fxmlFile;
    }
}