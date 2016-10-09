package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ClientMessageListener;
import ru.nsu.likhachev.network.filetransfer.MessageHandler;

/**
 * Client-sent message interface.
 *
 * Copyright (c) 2016 Alexander Likhachev.
 */
public interface ClientMessage extends Message {
    /**
     * Designed to delegate business logic to listener.
     * {@link MessageHandler} can be used to send responses to the client.
     *
     * @param listener listener of the message
     * @param messageHandler user messages handler
     */
    void handle(ClientMessageListener listener, MessageHandler messageHandler);
}
