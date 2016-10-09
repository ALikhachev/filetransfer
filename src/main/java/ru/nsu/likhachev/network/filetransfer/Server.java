package ru.nsu.likhachev.network.filetransfer;

import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileData;
import ru.nsu.likhachev.network.filetransfer.messages.CMessageFileMetadata;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileDataStatus;
import ru.nsu.likhachev.network.filetransfer.messages.SMessageFileMetadataStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Copyright (c) 2016 Alexander Likhachev.
 */
public class Server implements ClientMessageListener {
    private final Selector selector;

    public Server() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(Constants.SERVER_PORT));
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("Hello!");
        System.out.println("I'm listening to " + channel.getLocalAddress());
    }

    private static int fileCounter = 0;
    private Map<Integer, String> fileIdToName = new HashMap<>();

    @Override
    public void messageMetadata(CMessageFileMetadata msg, MessageHandler messageHandler) {
        try {
            File uploadsDir = new File("uploads");
            this.fileIdToName.put(fileCounter, msg.getFilename());
            if (!uploadsDir.exists() && !uploadsDir.mkdirs()) {
                System.out.println("Cannot create uploads dir");
                messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter++, true, "Cannot create uploads dir", msg.getFilename()));
                return;
            }
            File file = new File("uploads/" + msg.getFilename());
            if (file.exists()) {
                System.out.println("File " + msg.getFilename() + " already exists");
                messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter++, true, "File already exists", msg.getFilename()));
                return;
            }
            try (RandomAccessFile raFile = new RandomAccessFile("uploads/" + msg.getFilename(), "rw")) {
                raFile.setLength(msg.getLength());
                System.out.println("Created file " + msg.getFilename() + " of size " + msg.getLength());
                messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter++, false, "OK", msg.getFilename()));
            }
        } catch (IOException e) {
            System.out.println("Cannot access file " + msg.getFilename());
            messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter++, true, "Cannot access file", msg.getFilename()));
        }
    }

    @Override
    public void messageFileData(CMessageFileData msg, MessageHandler messageHandler) {
        try {
            try (RandomAccessFile raFile = new RandomAccessFile("uploads/" + this.fileIdToName.get(msg.getFileId()), "rw")) {
                raFile.seek(msg.getIndex() * Constants.FILE_PIECE_SIZE);
                raFile.write(msg.getData());
                System.out.println("Write " + msg.getLen() + " of data by padding " + msg.getIndex() * Constants.FILE_PIECE_SIZE
                        + " (index " + msg.getIndex() + ")");
                messageHandler.queueMessage(new SMessageFileDataStatus(msg.getFileId(), msg.getIndex(), false, "OK"));
            }
        } catch (IOException e) {
            messageHandler.queueMessage(new SMessageFileDataStatus(msg.getFileId(), msg.getIndex(), true, e.getMessage()));
        }
    }

    public void listen() throws IOException {
        for(;;) {
            int selected = this.selector.select();
            if (selected == 0) {
                continue;
            }
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                if (!key.isValid()) {
                    key.channel().close();
                    key.cancel();
                }
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = channel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new MessageHandler(this));
                    }
                    if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        MessageHandler handler = (MessageHandler) key.attachment();
                        handler.write(client);
                    }
                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        MessageHandler handler = (MessageHandler) key.attachment();
                        handler.read(client);
                    }
                } catch (ClosedChannelException ex) {
                    System.err.println("Client disconnected");
                    key.cancel();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.err.println("Lost connection with client");
                    key.cancel();
                    key.channel().close();
                }
                selectedKeys.remove();
            }
        }
    }

    private static String stringifySpeed(long bytes) {
        if (bytes == 0) {
            return "0 (or something too small)";
        }
        String[] units = {"bit", "kb", "Mb", "Gb", "Tb", "Pb"};
        bytes *= 8;
        int number = (int) Math.floor(Math.log(bytes) / Math.log(1024));

        return new DecimalFormat("#.#").format(bytes / Math.pow(1024, Math.floor(number))) + ' ' + units[number] + "/s";
    }

    public static void main(String args[]) {
        try {
            Server serv = new Server();
            serv.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
