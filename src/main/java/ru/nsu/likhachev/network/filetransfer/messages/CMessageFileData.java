package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ClientMessageListener;
import ru.nsu.likhachev.network.filetransfer.MessageHandler;

import java.nio.ByteBuffer;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class CMessageFileData implements ClientMessage {
    private int fileId;
    private int index;
    private int len;
    private byte[] data;

    public CMessageFileData() {

    }

    public CMessageFileData(int fileId, int index, byte[] data) {
        this.fileId = fileId;
        this.index = index;
        this.len = data.length;
        this.data = data;
    }

    @Override
    public void handle(ClientMessageListener listener, MessageHandler messageHandler) {
        listener.messageFileData(this, messageHandler);
    }

    @Override
    public void readData(ByteBuffer buf) {
        this.fileId = buf.getInt();
        this.index = buf.getInt();
        this.len = buf.getInt();
        byte[] bytes = new byte[this.len];
        buf.get(bytes);
        this.data = bytes;
    }

    @Override
    public void writeData(ByteBuffer buf) {
        buf.putInt(this.fileId);
        buf.putInt(this.index);
        buf.putInt(this.len);
        buf.put(this.data);
    }

    @Override
    public byte getId() {
        return 1;
    }

    public int getFileId() {
        return fileId;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }

    public int getLen() {
        return len;
    }
}
