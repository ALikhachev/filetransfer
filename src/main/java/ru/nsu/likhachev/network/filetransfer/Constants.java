package ru.nsu.likhachev.network.filetransfer;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class Constants {
    public final static int SERVER_PORT = 14202;
    public final static String CLIENT_REMOTE_HOST = "localhost";
    public final static int CLIENT_REMOTE_PORT = 14202;

    public final static int BUFFER_SIZE = 1024 * 1024 * 10;
    public final static int FILE_PIECE_SIZE = 1024 * 1024;
}
