package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NetworkClient {
    private SocketChannel channel;
    private final ByteBuffer readBuffer;
    private Consumer<NetworkMessage> messageHandler; // Теперь принимает объект, а не строку
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
    private boolean connected;
    private String host;
    private int port;

    public NetworkClient() {
        // Увеличим буфер для больших JSON (состояние игрового поля)
        this.readBuffer = ByteBuffer.allocate(16384);
        this.executor = Executors.newSingleThreadExecutor();
        this.objectMapper = new ObjectMapper();
        this.connected = false;
    }

    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));

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
        System.out.println("Подключено к серверу по протоколу [Length+JSON] " + host + ":" + port);
    }

    public void disconnect() {
        connected = false;
        try {
            if (channel != null) channel.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии канала: " + e.getMessage());
        }
        executor.shutdownNow();
    }

    /**
     * Исправлено: Отправка по протоколу [4 байта длины] + [JSON]
     */
    public void sendMessage(String type, String payload) {
        if (!isConnected()) return;

        try {
            NetworkMessage message = new NetworkMessage(type, payload);
            byte[] body = objectMapper.writeValueAsBytes(message);

            // Выделяем память: 4 байта под int + длина тела
            ByteBuffer writeBuffer = ByteBuffer.allocate(4 + body.length);
            writeBuffer.putInt(body.length); // Первые 4 байта — длина
            writeBuffer.put(body);           // Само сообщение
            writeBuffer.flip();

            while (writeBuffer.hasRemaining()) {
                channel.write(writeBuffer);
            }
        } catch (IOException e) {
            handleSystemError("Ошибка отправки: " + e.getMessage());
        }
    }

    /**
     * Исправлено: Потоковое чтение с учетом склейки/разрезания пакетов
     */
    private void startMessageReader() {
        executor.submit(() -> {
            try {
                while (connected && channel.isOpen()) {
                    int bytesRead = channel.read(readBuffer);

                    if (bytesRead == -1) {
                        handleDisconnect();
                        break;
                    }

                    if (bytesRead > 0) {
                        processBuffer();
                    }
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                handleSystemError("Сетевая ошибка: " + e.getMessage());
            }
        });
    }

    private void processBuffer() {
        readBuffer.flip(); // Переходим в режим чтения из буфера

        while (readBuffer.remaining() >= 4) {
            readBuffer.mark(); // Запоминаем позицию начала длины
            int length = readBuffer.getInt();

            if (readBuffer.remaining() < length) {
                // Если все тело сообщения еще не дошло, откатываемся и ждем
                readBuffer.reset();
                break;
            }

            // Читаем тело сообщения
            byte[] body = new byte[length];
            readBuffer.get(body);

            try {
                NetworkMessage msg = objectMapper.readValue(body, NetworkMessage.class);
                Platform.runLater(() -> {
                    if (messageHandler != null) messageHandler.accept(msg);
                });
            } catch (IOException e) {
                System.err.println("Ошибка парсинга JSON: " + e.getMessage());
            }
        }
        readBuffer.compact(); // Сохраняем остатки в начало буфера
    }

    private void handleDisconnect() {
        connected = false;
        Platform.runLater(() -> {
            if (messageHandler != null) {
                messageHandler.accept(new NetworkMessage("DISCONNECTED", "Сервер разорвал соединение"));
            }
        });
    }

    private void handleSystemError(String error) {
        connected = false;
        Platform.runLater(() -> {
            if (messageHandler != null) {
                messageHandler.accept(new NetworkMessage("ERROR", error));
            }
        });
    }

    public void setMessageHandler(Consumer<NetworkMessage> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return connected && channel != null && channel.isConnected();
    }
}