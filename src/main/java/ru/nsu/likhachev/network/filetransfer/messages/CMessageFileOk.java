package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ClientMessageListener;
import ru.nsu.likhachev.network.filetransfer.MessageHandler;

import java.nio.ByteBuffer;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class CMessageFileOk implements ClientMessage {
    private int fileId;


    public CMessageFileOk() {

    }

    public CMessageFileOk(int fileId) {
        this.fileId = fileId;
    }

    @Override
    public void handle(ClientMessageListener listener, MessageHandler messageHandler) {
        listener.messageFileOk(this, messageHandler);
    }

    @Override
    public void readData(ByteBuffer buf) {
        this.fileId = buf.getInt();
    }

    @Override
    public void writeData(ByteBuffer buf) {
        buf.putInt(this.fileId);
    }

    @Override
    public byte getId() {
        return 2;
    }

    public int getFileId() {
        return this.fileId;
    }
}
