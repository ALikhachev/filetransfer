package ru.nsu.likhachev.network.filetransfer.messages;

import java.nio.ByteBuffer;

/**
 * Data Transfer Object (DTO) interface.
 *
 * Copyright (c) 2016 Alexander Likhachev.
 */
public interface Message {
    /**
     * Deserializes the message from byte-sequence.
     *
     * It starts from current position of buffer and advanced pointer to N bytes,
     * where N is a size of message.
     *
     * @param buf ByteBuffer to deserialize from
     */
    void readData(ByteBuffer buf);

    /**
     * Serializes the message into byte-sequence.
     *
     * It starts from current position of buffer and advanced pointer to N bytes,
     * where N is a size of message.
     *
     * @param buf ByteBuffer to serialize into
     */
    void writeData(ByteBuffer buf);

    /**
     * Returns id of the message.
     *
     * @return id (0-127)
     */
    byte getId();
}
