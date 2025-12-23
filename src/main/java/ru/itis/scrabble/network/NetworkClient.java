package ru.itis.scrabble.network;

import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NetworkClient {
    private SocketChannel channel;
    private ByteBuffer buffer;
    private Consumer<String> messageHandler;
    private ExecutorService executor;
    private boolean connected;
    private String host;
    private int port;

    public NetworkClient() {
        this.buffer = ByteBuffer.allocate(4096);
        this.executor = Executors.newSingleThreadExecutor();
        this.connected = false;
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
        startMessageReader();
        System.out.println("Подключено к серверу " + host + ":" + port);
    }

    public void disconnect() {
        connected = false;
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

    public void sendMessage(String message) {
        if (!connected || channel == null || !channel.isConnected()) {
            System.err.println("Не подключено к серверу, сообщение не отправлено: " + message);
            return;
        }

        try {
            ByteBuffer sendBuffer = ByteBuffer.wrap((message + "\n").getBytes());
            channel.write(sendBuffer);
            System.out.println("Отправлено на сервер: " + message);
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
            connected = false;
        }
    }

    public void sendCommand(String command, String data) {
        String message = command + "|" + data;
        sendMessage(message);
    }

    private void startMessageReader() {
        executor.submit(() -> {
            while (connected && channel != null && channel.isConnected()) {
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