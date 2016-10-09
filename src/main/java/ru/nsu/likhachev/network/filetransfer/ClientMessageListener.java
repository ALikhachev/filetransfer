package ru.nsu.likhachev.network.filetransfer;

import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileData;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileMetadata;

/**
 * Client-sent messages listener interface
 *
 * Copyright (c) 2016 Alexander Likhachev.
 */
public interface ClientMessageListener {
    /**
     * Does some processing of received file metadata message.
     *
     * @param msg the message
     * @param messageHandler user messages handler
     */
    void messageMetadata(CMessageFileMetadata msg, MessageHandler messageHandler);

    /**
     * Does some processing of received file piece message.
     *
     * @param msg the message
     * @param messageHandler user messages handler
     */
    void messageFileData(CMessageFileData msg, MessageHandler messageHandler);
}
