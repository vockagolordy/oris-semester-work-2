package ru.itis.scrabble;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.itis.scrabble.navigation.NavigationManager;
import ru.itis.scrabble.network.NetworkClient;
import ru.itis.scrabble.navigation.View;

public class ClientLauncher extends Application {

    private NavigationManager navigationManager;
    private NetworkClient networkClient;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Настраиваем главное окно
            primaryStage.setTitle("Scrabble Game - Сетевая версия");
            primaryStage.setWidth(1000);
            primaryStage.setHeight(700);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // 2. Инициализируем сетевой клиент
            String serverHost = "localhost"; // Можно вынести в конфиг или аргументы командной строки
            int serverPort = 8080;

            networkClient = new NetworkClient();

            // 3. Инициализируем менеджер навигации
            navigationManager = new NavigationManager(primaryStage);
            navigationManager.setNetworkService(networkClient);

            // 4. Подключаемся к серверу (асинхронно, чтобы не блокировать UI)
            connectToServerAsync(serverHost, serverPort);

            // 5. Запускаем приложение с экрана логина
            navigationManager.navigate(View.LOGIN);

        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Ошибка запуска",
                "Не удалось запустить приложение:\n" + e.getMessage());
        }
    }

    private void connectToServerAsync(String host, int port) {
        // Запускаем подключение в отдельном потоке, чтобы не блокировать UI
        new Thread(() -> {
            try {
                System.out.println("Попытка подключения к серверу " + host + ":" + port + "...");
                networkClient.connect(host, port);
                System.out.println("Успешно подключено к серверу: " + host + ":" + port);

                // После успешного подключения можно отправить тестовое сообщение
                // или перейти на другой экран, если требуется

            } catch (Exception e) {
                System.err.println("Ошибка подключения к серверу: " + e.getMessage());

                // Показываем ошибку в UI потоке
                javafx.application.Platform.runLater(() -> {
                    showErrorDialog("Ошибка подключения",
                        "Не удалось подключиться к игровому серверу.\n\n" +
                        "Сервер: " + host + ":" + port + "\n" +
                        "Причина: " + e.getMessage() + "\n\n" +
                        "Проверьте:\n" +
                        "1. Запущен ли игровой сервер\n" +
                        "2. Правильность адреса и порта\n" +
                        "3. Настройки фаервола/сети\n\n" +
                        "Вы можете продолжить в автономном режиме, но многопользовательская игра будет недоступна.");

                    // Можно предложить использовать локальный режим
                    offerOfflineMode();
                });
            }
        }).start();
    }

    private void offerOfflineMode() {
        // Можно предложить пользователю играть в одиночку против ИИ
        // или в локальной сети
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION
        );
        alert.setTitle("Автономный режим");
        alert.setHeaderText("Подключение к серверу не удалось");
        alert.setContentText("Хотите продолжить в автономном режиме?\n" +
                           "Вы сможете играть против компьютера или настроить локальную игру.");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Переходим в автономный режим
                // Можно создать локальный сервер или перейти к игре с ИИ
                navigationManager.showDialog("Автономный режим",
                    "В разработке. Пожалуйста, настройте подключение к серверу.");
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        // Корректно завершаем работу при закрытии приложения
        System.out.println("Завершение работы приложения...");

        if (networkClient != null) {
            try {
                // Отправляем сообщение о выходе, если пользователь авторизован
                if (navigationManager != null) {
                    Long userId = null; // Здесь нужно получить текущий userId
                    // Можно добавить метод getCurrentUserId() в NavigationManager

                    if (userId != null) {
                        // Отправляем команду выхода на сервер
                        networkClient.sendMessage("LOGOUT|" + userId);
                    }
                }

                // Отключаемся от сервера
                networkClient.disconnect();
                System.out.println("Сетевое соединение закрыто");

            } catch (Exception e) {
                System.err.println("Ошибка при отключении от сервера: " + e.getMessage());
            }
        }

        System.out.println("Приложение завершено");
    }

    public static void main(String[] args) {
        // Обработка аргументов командной строки
        String serverHost = "localhost";
        int serverPort = 8080;

        if (args.length >= 2) {
            try {
                serverHost = args[0];
                serverPort = Integer.parseInt(args[1]);
                System.out.println("Используется сервер: " + serverHost + ":" + serverPort);
            } catch (NumberFormatException e) {
                System.err.println("Неверный формат порта. Используется порт по умолчанию: " + serverPort);
            }
        } else {
            System.out.println("Используется сервер по умолчанию: " + serverHost + ":" + serverPort);
            System.out.println("Для указания другого сервера: java -jar scrabble.jar <хост> <порт>");
        }

        // Сохраняем параметры для использования в приложении
        System.setProperty("scrabble.server.host", serverHost);
        System.setProperty("scrabble.server.port", String.valueOf(serverPort));

        // Запускаем JavaFX приложение
        launch(args);
    }
}