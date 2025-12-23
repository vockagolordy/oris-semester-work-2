package ru.itis.scrabble.network;

/**
 * Единая модель сообщения для протокола.
 */
public class NetworkMessage {
    private MessageType type;
    private String payload;

    public NetworkMessage() {}

    public NetworkMessage(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}