package ru.nsu.likhachev.network.filetransfer;

import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileData;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileOk;
import ru.nsu.likhachev.network.filetransfer.messages.ClientMessage;
import ru.nsu.likhachev.network.filetransfer.messages.Message;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileMetadata;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileDataStatus;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileMetadataStatus;
import ru.nsu.likhachev.network.filetransfer.messages.ServerMessage;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class MessageHandler {
    private final ByteBuffer recvBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
    private final Map<Byte, Class<? extends Message>> messages;
    private final Queue<Message> messagesQueue = new LinkedList<>();
    private final ClientMessageListener server;
    private final ServerMessageListener client;

    public MessageHandler(ClientMessageListener server) {
        this.messages = CLIENT_MESSAGES;
        this.server = server;
        this.client = null;
    }

    public MessageHandler(ServerMessageListener client) {
        this.messages = SERVER_MESSAGES;
        this.server = null;
        this.client = client;
    }

    public void read(SocketChannel channel) throws IOException {
        if (channel.read(this.recvBuffer) < 0) {
            channel.close();
            return;
        }
        this.recvBuffer.flip();
        while (this.recvBuffer.remaining() > 0) {
            try {
                this.recvBuffer.mark();
                int start = this.recvBuffer.position();
                byte messageId = this.recvBuffer.get();
                Class<? extends Message> messageClass = this.messages.get(messageId);
                if (messageClass == null) {
                    throw new IOException("Unknown message id: " + messageId);
                }
                Message msg = messageClass.newInstance();
                msg.readData(this.recvBuffer);
                System.out.println("[" + channel.getRemoteAddress() + "] Read " + (this.recvBuffer.position() - start)
                        + " bytes of " + msg.getClass().getName());
                if (msg instanceof ClientMessage) {
                    ((ClientMessage) msg).handle(server, this);
                } else if (msg instanceof ServerMessage) {
                    ((ServerMessage) msg).handle(client);
                }
            } catch (BufferUnderflowException ex) {
                this.recvBuffer.reset();
                break;
            } catch (InstantiationException | IllegalAccessException ignored) {
            }
        }
        this.recvBuffer.compact();
        this.recvBuffer.limit(this.recvBuffer.capacity());
    }

    public void write(SocketChannel channel) throws IOException {
        this.unqueueMessages(channel.getRemoteAddress().toString());
        if (this.sendBuffer.position() > 0) {
            this.sendBuffer.flip();
            channel.write(this.sendBuffer);
            this.sendBuffer.compact();
            this.sendBuffer.limit(this.sendBuffer.capacity());
        }
    }

    public void queueMessage(Message msg) {
        this.messagesQueue.add(msg);
    }

    private void unqueueMessages(String prefix) {
        int safePosition = this.sendBuffer.position();
        try {
            while (!messagesQueue.isEmpty()) {
                safePosition = this.sendBuffer.position();
                Message msg = this.messagesQueue.peek();
                int start = this.sendBuffer.position();
                this.sendBuffer.put(msg.getId());
                msg.writeData(this.sendBuffer);
                System.out.println("[" + prefix + "] Write " + (this.sendBuffer.position() - start)
                        + " bytes of " + msg.getClass().getName());
                this.messagesQueue.poll();
            }
        } catch (BufferOverflowException ex) {
            this.sendBuffer.position(safePosition);
        }
    }

    private final static Map<Byte, Class<? extends Message>> CLIENT_MESSAGES = new HashMap<>();
    private final static Map<Byte, Class<? extends Message>> SERVER_MESSAGES = new HashMap<>();

    static {
        CLIENT_MESSAGES.put((byte) 0, CMessageFileMetadata.class);
        CLIENT_MESSAGES.put((byte) 1, CMessageFileData.class);
        CLIENT_MESSAGES.put((byte) 2, CMessageFileOk.class);

        SERVER_MESSAGES.put((byte) 0, SMessageFileMetadataStatus.class);
        SERVER_MESSAGES.put((byte) 1, SMessageFileDataStatus.class);
    }
}
