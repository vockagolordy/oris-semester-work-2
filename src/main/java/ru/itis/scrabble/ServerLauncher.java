package ru.itis.scrabble;

import ru.itis.scrabble.network.NetworkServer;
import ru.itis.scrabble.network.PacketHandler;
import ru.itis.scrabble.services.*;

import java.util.Scanner;

public class ServerLauncher {
    public static void main(String[] args) {
        // 1. Инициализация атомарных сервисов
        BagService bagService = new BagServiceImpl();
        BoardService boardService = new BoardServiceImpl();
        WordService wordService = new WordServiceImpl();
        ScoringService scoringService = new ScoringServiceImpl();

        // 2. Инициализация ключевого гейм-сервиса
        GameSessionService gameSessionService = new GameSessionServiceImpl(
                boardService,
                wordService,
                scoringService,
                bagService
        );

        // 3. Создание диспетчера пакетов
        PacketHandler packetHandler = new PacketHandler(gameSessionService);

        // 4. Запуск сетевого сервера на порту 8080
        int port = 8080;
        NetworkServer server = new NetworkServer(port);

        // Передаем обработчик в сервер (нужно добавить сеттер в NetworkServer)
        server.setPacketHandler(packetHandler);

        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("=== SCRABBLE SERVER STARTED ===");
        System.out.println("Listening on port: " + port);
        System.out.println("Type 'exit' to stop the server.");

        // Консоль управления сервером
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String command = scanner.nextLine();
            if ("exit".equalsIgnoreCase(command)) {
                server.stop();
                break;
            }
        }
    }
}