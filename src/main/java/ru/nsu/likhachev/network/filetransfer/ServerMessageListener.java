package ru.nsu.likhachev.network.filetransfer;

import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileDataStatus;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileMetadataStatus;

/**
 *  Server-sent messages listener interface
 *
 * Copyright (c) 2016 Alexander Likhachev.
 */
public interface ServerMessageListener {
    /**
     * Does some processing of received file metadata status message.
     *
     * {@link SMessageFileMetadataStatus#getFileId()} returns the file id that
     * the server associated with the {@link SMessageFileMetadataStatus#getFilename}.
     *
     * {@link SMessageFileMetadataStatus#isError()} becomes truly if some
     * errors are occurred on the server-side. Otherwise, it always false and
     * {@link SMessageFileMetadataStatus#getMsg()} is "OK"
     *
     * @param msg the message
     */
    void messageStatus(SMessageFileMetadataStatus msg);

    /**
     * Does some processing of received file piece status message.
     *
     * {@link SMessageFileMetadataStatus#isError()} becomes truly if some
     * errors are occurred on the server-side during saving of piece at {@link SMessageFileDataStatus#getIndex()}} index
     * Otherwise, it always false and {@link SMessageFileMetadataStatus#getMsg()} is "OK"
     *
     * @param msg the message
     */
    void messageDataStatus(SMessageFileDataStatus msg);
}
