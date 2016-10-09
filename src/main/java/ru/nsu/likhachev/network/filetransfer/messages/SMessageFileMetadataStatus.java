package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ServerMessageListener;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class SMessageFileMetadataStatus implements ServerMessage {
    private int fileId;
    private boolean error;
    private int msgLength;
    private String msg;
    private int filenameLength;
    private String filename;

    public SMessageFileMetadataStatus() {

    }

    public SMessageFileMetadataStatus(int fileId, boolean err, String msg, String filename) {
        this.fileId = fileId;
        this.error = err;
        this.msg = msg;
        this.msgLength = msg.getBytes(StandardCharsets.UTF_8).length;
        this.filename = msg;
        this.filenameLength = filename.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public void readData(ByteBuffer buf) {
        this.fileId = buf.getInt();
        this.error = buf.get() != 0;
        this.msgLength = buf.getInt();
        byte[] bytes = new byte[this.msgLength];
        buf.get(bytes);
        this.msg = new String(bytes, StandardCharsets.UTF_8);
        this.filenameLength = buf.getInt();
        bytes = new byte[this.filenameLength];
        buf.get(bytes);
        this.filename = new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void writeData(ByteBuffer buf) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        buf.putInt(this.fileId);
        buf.put(this.error ? (byte) 1 : 0);
        buf.putInt(msgBytes.length);
        buf.put(msgBytes);
        buf.putInt(filenameBytes.length);
        buf.put(filenameBytes);
    }

    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public void handle(ServerMessageListener listener) {
        listener.messageStatus(this);
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

    public String getFilename() {
        return filename;
    }
}
