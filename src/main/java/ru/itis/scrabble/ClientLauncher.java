package ru.itis.scrabble;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.itis.scrabble.network.NetworkClient;

public class ClientLauncher extends Application {

    private NetworkClient networkClient;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Инициализируем сетевое соединение (TODO: вынести хост и порт в конфиг)
        networkClient = new NetworkClient("localhost", 8080);

        // Запускаем поток прослушивания сервера
        Thread clientThread = new Thread(networkClient);
        clientThread.setDaemon(true);
        clientThread.start();

        // 2. Загружаем UI
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_board.fxml"));
        Parent root = loader.load();

        // 3. Передаем клиента в контроллер для взаимодействия
        // TODO: Реализовать получение контроллера и передачу в него networkClient
        // MainController controller = loader.getController();
        // controller.setNetworkClient(networkClient);
        // networkClient.setHandler(controller);

        primaryStage.setTitle("Scrabble - Итис");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (networkClient != null) {
            networkClient.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}