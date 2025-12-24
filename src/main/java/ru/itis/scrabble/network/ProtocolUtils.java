package ru.itis.scrabble.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itis.scrabble.dto.NetworkMessageDTO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for length-prefixed JSON protocol: [4-byte length][JSON bytes]
 */
public final class ProtocolUtils {
    private ProtocolUtils() {}

    public static ByteBuffer encode(NetworkMessageDTO msg, ObjectMapper mapper) throws IOException {
        byte[] body = mapper.writeValueAsBytes(msg);
        ByteBuffer buffer = ByteBuffer.allocate(4 + body.length);
        buffer.putInt(body.length);
        buffer.put(body);
        buffer.flip();
        return buffer;
    }

    /**
     * Decode as many complete messages as present in the provided buffer.
     * The buffer must be in read mode (flip() already called). After return the buffer is left in read mode
     * with position after the last read byte; caller should compact() as needed.
     */
    public static List<NetworkMessageDTO> decode(ByteBuffer buffer, ObjectMapper mapper) throws IOException {
        List<NetworkMessageDTO> messages = new ArrayList<>();

        while (buffer.remaining() >= 4) {
            buffer.mark();
            int length = buffer.getInt();

            if (length <= 0) {
                buffer.reset();
                throw new IOException("Invalid message length: " + length);
            }

            if (buffer.remaining() < length) {
                buffer.reset();
                break; // wait for more data
            }

            byte[] body = new byte[length];
            buffer.get(body);
            NetworkMessageDTO msg = mapper.readValue(body, NetworkMessageDTO.class);
            messages.add(msg);
        }

        return messages;
    }
}
