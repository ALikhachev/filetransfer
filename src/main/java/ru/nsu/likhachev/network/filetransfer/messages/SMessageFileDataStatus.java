package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ServerMessageListener;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class SMessageFileDataStatus implements ServerMessage {
    private int fileId;
    private int index;
    private boolean error;
    private int msgLength;
    private String msg;

    public SMessageFileDataStatus() {

    }

    public SMessageFileDataStatus(int fileId, int index, boolean err, String msg) {
        this.fileId = fileId;
        this.index = index;
        this.error = err;
        this.msg = msg;
        this.msgLength = msg.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void readData(ByteBuffer buf) {
        this.fileId = buf.getInt();
        this.index = buf.getInt();
        this.error = buf.get() != 0;
        this.msgLength = buf.getInt();
        ByteBuffer tmpBuf = ByteBuffer.allocate(this.msgLength);
        for (int i = 0; i < this.msgLength; ++i) {
            tmpBuf.put(buf.get());
        }
        this.msg = new String(tmpBuf.array(), StandardCharsets.UTF_8);
    }

    @Override
    public void writeData(ByteBuffer buf) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        buf.putInt(this.fileId);
        buf.putInt(this.index);

        buf.put(this.error ? (byte) 1 : 0);
        buf.putInt(msgBytes.length);
        buf.put(msgBytes);
    }

    @Override
    public byte getId() {
        return 1;
    }

    @Override
    public void handle(ServerMessageListener listener) {
        listener.messageDataStatus(this);
    }

    public int getFileId() {
        return fileId;
    }

    public boolean isError() {
        return error;
    }

    public int getMsgLength() {
        return msgLength;
    }

    public String getMsg() {
        return msg;
    }

    public int getIndex() {
        return index;
    }
}