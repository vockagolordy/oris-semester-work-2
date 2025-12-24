package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import javafx.application.Platform;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import ru.itis.scrabble.dto.NetworkMessageDTO;

public class NetworkClient {
    private SocketChannel channel;
    private final ByteBuffer readBuffer;
    private Consumer<NetworkMessageDTO> messageHandler; // Теперь принимает dto.NetworkMessage
    private ExecutorService executor;
    private final ObjectMapper objectMapper;
    private boolean connected;
    private String host;
    private int port;
    private static final int DEFAULT_READ_BUFFER = 64 * 1024; // 64KB
    // Maximum message we can hold in the read buffer (reserve 4 bytes for length)
    private static final int MAX_MESSAGE_SIZE = DEFAULT_READ_BUFFER - 4;

    public NetworkClient() {
        // Увеличим буфер для больших JSON (состояние игрового поля)
        this.readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER);
        this.objectMapper = new ObjectMapper();
        // Be tolerant to small DTO changes from server
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        // Switch to blocking mode for reliable blocking reads/writes on the background thread
        channel.configureBlocking(true);

        // Create executor per-connection so reconnects work correctly
        this.executor = Executors.newSingleThreadExecutor();

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
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * Исправлено: Отправка по протоколу [4 байта длины] + [JSON]
     */
    public void sendMessage(String type, String payload) {
        if (!isConnected()) return;

        MessageType mt;
        try {
            mt = MessageType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            mt = MessageType.GAME_EVENT;
        }

        sendMessage(mt, payload);
    }

    public void sendMessage(MessageType mt, String payload) {
        if (!isConnected()) return;
        try {
            NetworkMessageDTO message = new NetworkMessageDTO(mt, payload, null);
            byte[] body = objectMapper.writeValueAsBytes(message);

            // Выделяем память: 4 байта под int + длина тела
            ByteBuffer writeBuffer = ByteBuffer.allocate(4 + body.length);
            writeBuffer.putInt(body.length); // Первые 4 байта — длина
            writeBuffer.put(body);           // Само сообщение
            writeBuffer.flip();

            synchronized (this) {
                while (writeBuffer.hasRemaining()) {
                    try {
                        channel.write(writeBuffer);
                    } catch (IOException e) {
                        handleSystemError("Ошибка записи в канал: " + e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            handleSystemError("Ошибка сериализации сообщения: " + e.getMessage());
        }
    }

    /**
     * Send asynchronously using the client's executor to avoid blocking caller threads (e.g., JavaFX thread).
     */
    public void sendMessageAsync(String type, String payload) {
        MessageType mt;
        try {
            mt = MessageType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            mt = MessageType.GAME_EVENT;
        }
        sendMessageAsync(mt, payload);
    }

    public void sendMessageAsync(MessageType mt, String payload) {
        if (executor == null || executor.isShutdown() || !isConnected()) {
            // fallback to synchronous send to not drop messages when executor unavailable
            sendMessage(mt, payload);
            return;
        }
        executor.submit(() -> sendMessage(mt, payload));
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
                    // In blocking mode, read() will block until data arrives or channel closes.
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

            // Basic sanity checks for length to avoid OOM or protocol errors
            if (length <= 0 || length > MAX_MESSAGE_SIZE) {
                // Protocol error: invalid length
                readBuffer.reset();
                handleSystemError("Protocol error: invalid message length: " + length);
                try {
                    if (channel != null) channel.close();
                } catch (IOException ignored) {}
                return;
            }

            if (readBuffer.remaining() < length) {
                // Если все тело сообщения еще не дошло, откатываемся и ждем
                readBuffer.reset();
                break;
            }

            // Читаем тело сообщения
            byte[] body = new byte[length];
            readBuffer.get(body);

            try {
                NetworkMessageDTO msg = objectMapper.readValue(body, NetworkMessageDTO.class);
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
                messageHandler.accept(new NetworkMessageDTO(MessageType.ERROR, "Сервер разорвал соединение", null));
            }
        });
    }

    private void handleSystemError(String error) {
        connected = false;
        Platform.runLater(() -> {
                if (messageHandler != null) {
                messageHandler.accept(new NetworkMessageDTO(MessageType.ERROR, error, null));
            }
        });
    }

    public void setMessageHandler(Consumer<NetworkMessageDTO> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return connected && channel != null && channel.isOpen();
    }
}