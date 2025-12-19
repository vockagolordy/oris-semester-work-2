// ServerLauncher.java
package ru.itis.scrabble.network;

import ru.itis.scrabble.services.*;
import ru.itis.scrabble.repositories.*;

public class ServerLauncher {
    public static void main(String[] args) {
        try {
            // Создаем репозитории (нужно реализовать)
            UserRepository userRepository = new InMemoryUserRepository();

            // Создаем сервисы (нужно реализовать имплементации)
            SecurityService securityService = new SecurityServiceImpl(userRepository);
            UserService userService = new UserServiceImpl(userRepository);
            RoomService roomService = new RoomServiceImpl();
            GameService gameService = new GameServiceImpl();
            BagService bagService = new BagServiceImpl();
            BoardService boardService = new BoardServiceImpl();
            ScoringService scoringService = new ScoringServiceImpl();

            // Создаем и запускаем сервер
            NetworkServer server = new NetworkServer(
                    securityService,
                    roomService,
                    userService,
                    gameService,
                    bagService,
                    boardService,
                    scoringService
            );

            // Запускаем основной порт (например, 8888)
            server.startServerOnPort(8888);
            server.start();

            System.out.println("Scrabble сервер запущен. Основной порт: 8888");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}