package ru.itis.scrabble.network;

/**
 * Единая модель сообщения для протокола.
 */
public class NetworkMessage {
    private String type;
    private String payload;

    public NetworkMessage() {}

    public NetworkMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}