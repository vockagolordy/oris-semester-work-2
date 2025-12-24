package ru.itis.scrabble.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class NetworkServer implements Runnable {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running;
    private PacketHandler packetHandler;

    // Мапа для хранения сессий: Канал -> Сессия
    private final Map<SocketChannel, ClientSession> sessions = new ConcurrentHashMap<>();

    public NetworkServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running = true;
            System.out.println("Сервер запущен на порту: " + port);

            while (running) {
                selector.select(); // Ожидание событий
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        registerClient(serverChannel);
                    }
                    if (key.isReadable()) {
                        readMessage(key);
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerClient(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        sessions.put(clientChannel, new ClientSession(clientChannel));
        System.out.println("Новое подключение: " + clientChannel.getRemoteAddress());
    }

    private void readMessage(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientSession session = sessions.get(channel);
        ByteBuffer buffer = session.getReadBuffer();

        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                disconnect(channel);
                return;
            }

            // Здесь будет логика разбора протокола [4 байта длины][JSON]
            processBuffer(session);

        } catch (IOException e) {
            disconnect(channel);
        }
    }

    private void disconnect(SocketChannel channel) {
        try {
            System.out.println("Клиент отключился: " + channel.getRemoteAddress());
            sessions.remove(channel);
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.running = false;
        if (selector != null) selector.wakeup();
    }

    // Метод для отправки сообщения клиенту
    public void sendMessage(SocketChannel channel, String json) throws IOException {
        byte[] body = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
        buffer.putInt(body.length);
        buffer.put(body);
        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void processBuffer(ClientSession session) {
        ByteBuffer buffer = session.getReadBuffer();

        buffer.flip();

        while (true) {
            if (buffer.remaining() < 4) {
                break;
            }

            buffer.mark();
            int payloadLength = buffer.getInt();

            // Sanity check to avoid invalid or malicious lengths
            if (payloadLength <= 0 || payloadLength > buffer.capacity() - 4) {
                System.err.println("Protocol error: invalid payload length: " + payloadLength + " in session " + session.getSessionId());
                disconnect(session.getChannel());
                return;
            }

            if (buffer.remaining() < payloadLength) {
                buffer.reset();
                break;
            }

            byte[] body = new byte[payloadLength];
            buffer.get(body);
            String json = new String(body, java.nio.charset.StandardCharsets.UTF_8);

            System.out.println("Получен пакет от сессии " + session.getSessionId() + ": " + json);
            handleJsonPacket(session, json);

        }


        buffer.compact();
    }


    public void setPacketHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    private void handleJsonPacket(ClientSession session, String json) {
        if (packetHandler != null) {
            packetHandler.handle(session, json);
        } else {
            System.err.println("Предупреждение: PacketHandler не установлен!");
        }
    }
}