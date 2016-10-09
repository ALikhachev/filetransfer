package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ClientMessageListener;
import ru.nsu.likhachev.network.filetransfer.MessageHandler;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class CMessageFileMetadata implements ClientMessage {
    private long length;
    private int filenameLength;
    private String filename;

    public CMessageFileMetadata() {

    }

    public CMessageFileMetadata(long length, String filename) {
        this.length = length;
        this.filenameLength = filename.getBytes(StandardCharsets.UTF_8).length;
        this.filename = filename;
    }

    @Override
    public void readData(ByteBuffer buf) {
        this.length = buf.getLong();
        this.filenameLength = buf.getInt();
        byte[] bytes = new byte[this.filenameLength];
        buf.get(bytes);
        this.filename = new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void writeData(ByteBuffer buf) {
        byte[] nameBytes = filename.getBytes(StandardCharsets.UTF_8);
        buf.putLong(length);
        buf.putInt(nameBytes.length);
        buf.put(nameBytes);
    }

    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public void handle(ClientMessageListener listener, MessageHandler messageHandler) {
        listener.messageMetadata(this, messageHandler);
    }

    public String getFilename() {
        return filename;
    }

    public long getLength() {
        return length;
    }
}
