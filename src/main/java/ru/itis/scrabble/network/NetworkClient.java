package ru.itis.scrabble.network;

import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NetworkClient implements Runnable {
    private SocketChannel channel;
    private ByteBuffer buffer;
    private Consumer<String> messageHandler;
    private ExecutorService executor;
    private boolean connected;
    private boolean running;
    private String host;
    private int port;

    // Конструктор с параметрами (для совместимости)
    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.buffer = ByteBuffer.allocate(4096);
        this.executor = Executors.newSingleThreadExecutor();
        this.connected = false;
        this.running = true;
    }

    // Конструктор без параметров (для гибкости)
    public NetworkClient() {
        this.buffer = ByteBuffer.allocate(4096);
        this.executor = Executors.newSingleThreadExecutor();
        this.connected = false;
        this.running = true;
    }

    // Метод для установки хоста и порта
    public void setConnectionParams(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        // Реализация Runnable для запуска в потоке
        try {
            connect(host, port);
        } catch (IOException e) {
            System.err.println("Ошибка при запуске клиента: " + e.getMessage());
            Platform.runLater(() -> {
                if (messageHandler != null) {
                    messageHandler.accept("CONNECT_ERROR|" + e.getMessage());
                }
            });
        }
    }

    public void connect() throws IOException {
        connect(this.host, this.port);
    }

    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));

        // Ждем подключения
        while (!channel.finishConnect()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Connection interrupted", e);
            }
        }

        connected = true;
        running = true;
        startMessageReader();
        System.out.println("Подключено к серверу " + host + ":" + port);

        // Уведомляем об успешном подключении
        Platform.runLater(() -> {
            if (messageHandler != null) {
                messageHandler.accept("CONNECT_SUCCESS|Подключено к " + host + ":" + port);
            }
        });
    }

    public void disconnect() {
        connected = false;
        running = false;
        if (channel != null && channel.isConnected()) {
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Ошибка при отключении: " + e.getMessage());
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
        System.out.println("Отключено от сервера");
    }

    // Метод stop для совместимости с вашим кодом
    public void stop() {
        disconnect();
    }

    public void sendMessage(String message) {
        if (!connected || channel == null || !channel.isConnected()) {
            System.err.println("Не подключено к серверу, сообщение не отправлено: " + message);

            // Пытаемся переподключиться
            try {
                reconnect();
                // После переподключения отправляем сообщение снова
                if (connected) {
                    sendMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Не удалось переподключиться: " + e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
            ByteBuffer sendBuffer = ByteBuffer.wrap((message + "\n").getBytes());
            channel.write(sendBuffer);
            System.out.println("Отправлено на сервер: " + message);
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
            connected = false;

            // Уведомляем UI об ошибке
            Platform.runLater(() -> {
                if (messageHandler != null) {
                    messageHandler.accept("SEND_ERROR|" + e.getMessage());
                }
            });
        }
    }

    private void reconnect() throws IOException, InterruptedException {
        System.out.println("Попытка переподключения к серверу...");
        disconnect();
        Thread.sleep(2000); // Ждем 2 секунды перед повторной попыткой
        connect(host, port);
    }

    public void sendCommand(String command, String data) {
        String message = command + "|" + data;
        sendMessage(message);
    }

    private void startMessageReader() {
        executor.submit(() -> {
            while (running && connected && channel != null && channel.isConnected()) {
                try {
                    buffer.clear();
                    int bytesRead = channel.read(buffer);

                    if (bytesRead == -1) {
                        // Соединение закрыто сервером
                        connected = false;
                        Platform.runLater(() -> {
                            if (messageHandler != null) {
                                messageHandler.accept("DISCONNECTED|Сервер отключился");
                            }
                        });
                        break;
                    }

                    if (bytesRead > 0) {
                        buffer.flip();
                        String received = new String(buffer.array(), 0, buffer.limit());

                        // Обрабатываем все сообщения в полученных данных
                        String[] messages = received.split("\n");
                        for (String msg : messages) {
                            if (msg.trim().isEmpty()) continue;

                            final String finalMsg = msg.trim();
                            System.out.println("Получено от сервера: " + finalMsg);

                            // Вызываем обработчик в UI потоке
                            Platform.runLater(() -> {
                                if (messageHandler != null) {
                                    messageHandler.accept(finalMsg);
                                }
                            });
                        }
                    }

                    Thread.sleep(10); // Небольшая пауза
                } catch (IOException e) {
                    System.err.println("Ошибка чтения от сервера: " + e.getMessage());
                    connected = false;
                    Platform.runLater(() -> {
                        if (messageHandler != null) {
                            messageHandler.accept("ERROR|Сетевая ошибка: " + e.getMessage());
                        }
                    });
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    // Для совместимости с вашим кодом
    public void setHandler(Consumer<String> handler) {
        setMessageHandler(handler);
    }

    public boolean isConnected() {
        return connected && channel != null && channel.isConnected();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}