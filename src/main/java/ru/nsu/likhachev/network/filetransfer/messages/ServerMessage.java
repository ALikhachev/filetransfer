package ru.nsu.likhachev.network.filetransfer.messages;

import ru.nsu.likhachev.network.filetransfer.ServerMessageListener;

/**
 * Server-sent message interface.
 *
 * Copyright (c) 2016 Alexander Likhachev.
 */
public interface ServerMessage extends Message {
    /**
     * Designed to delegate business logic to listener.
     *
     * @param listener listener of the message
     */
    void handle(ServerMessageListener listener);
}
