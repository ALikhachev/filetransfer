package ru.nsu.likhachev.network.filetransfer;

import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileData;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileMetadata;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileOk;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileDataStatus;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileMetadataStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class Client implements ServerMessageListener {
    private final SocketChannel channel;
    private final MessageHandler packetHandler = new MessageHandler(this);
    private String filename;

    public Client(String host, int port, String filename) throws IOException {
        this.channel = SocketChannel.open(new InetSocketAddress(host, port));
        this.filename = filename;
    }

    @Override
    public void messageStatus(SMessageFileMetadataStatus msg) {
        if (msg.isError()) {
            throw new RuntimeException(msg.getMsg());
        }
        queuePiece(msg.getFileId(), 0);
    }

    @Override
    public void messageDataStatus(SMessageFileDataStatus msg) {
        if (msg.isError()) {
            System.err.println("Error during file transfer");
            throw new RuntimeException(msg.getMsg());
        }
        queuePiece(msg.getFileId(), msg.getIndex() + 1);
    }

    private void queuePiece(int fileId, int index) {
        try (RandomAccessFile raFile = new RandomAccessFile(this.filename, "rw")) {
            raFile.seek(index * Constants.FILE_PIECE_SIZE);
            byte[] buf = new byte[Constants.FILE_PIECE_SIZE];
            if (index >= (double) raFile.length() / (double) Constants.FILE_PIECE_SIZE) {
                System.out.println("File successfully transferred");
                throw new RuntimeException("ok");
            }
            int totalRead = 0;
            int read;
            while ((read = raFile.read(buf, totalRead, buf.length - totalRead)) > 0) {
                totalRead += read;
            }
            this.packetHandler.queueMessage(new CMessageFileData(fileId, index, Arrays.copyOfRange(buf, 0, totalRead)));
            if ((double) raFile.length() / (double) Constants.FILE_PIECE_SIZE - index < 1) {
                System.out.println(index);
                this.packetHandler.queueMessage(new CMessageFileOk(fileId));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file");
        }
    }

    public void eventLoop() throws IOException {
        File file = new File(this.filename);
        Selector selector = Selector.open();
        this.channel.configureBlocking(false);
        this.channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        this.packetHandler.queueMessage(new CMessageFileMetadata(file.length(), file.getName()));
        try {
            for (; ; ) {
                if (selector.select() == 0) {
                    continue;
                }
                Iterator<SelectionKey> itr = selector.selectedKeys().iterator();
                while (itr.hasNext()) {
                    SelectionKey key = itr.next();
                    if (!key.isValid()) {
                        key.channel().close();
                        key.cancel();
                    }
                    if (key.isWritable()) {
                        this.packetHandler.write((SocketChannel) key.channel());
                    }
                    if (key.isReadable()) {
                        this.packetHandler.read((SocketChannel) key.channel());
                    }
                    itr.remove();
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
            this.channel.close();
        }
    }

    public static void main(String args[]) {
        try {
            if (args.length < 1) {
                System.err.println("Usage: <filename> [hostname] [port]");
                return;
            }
            String filename = args[0];
            String host = args.length > 1 ? args[1] : Constants.CLIENT_REMOTE_HOST;
            int port;
            try {
                port = args.length > 2 ? Integer.parseInt(args[2]) : Constants.CLIENT_REMOTE_PORT;
            } catch (NumberFormatException ex) {
                port = Constants.CLIENT_REMOTE_PORT;
            }
            Client cl = new Client(host, port, filename);
            cl.eventLoop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
