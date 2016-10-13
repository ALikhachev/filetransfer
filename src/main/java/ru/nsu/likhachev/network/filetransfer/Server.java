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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
    private Map<Integer, RandomAccessFile> fileIdToFile = new HashMap<>();
    private Map<MessageHandler, Set<Integer>> handlerFiles = new HashMap<>();

    @Override
    public void messageMetadata(CMessageFileMetadata msg, MessageHandler messageHandler) {
        try {
            File uploadsDir = new File("uploads");
            File file = new File(uploadsDir, msg.getFilename().replace(":", ""));

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                System.out.println("Cannot create uploads dir");
                messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter, true, "Cannot create uploads dir", msg.getFilename()));
                return;
            }

            if (file.exists()) {
                System.out.println("File " + file.toString() + " already exists");
                messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter, true, "File already exists", msg.getFilename()));
                return;
            }
            RandomAccessFile raFile = new RandomAccessFile(file.toString(), "rw");
            this.fileIdToFile.put(fileCounter, raFile);
            Set<Integer> set = this.handlerFiles.get(messageHandler);
            if (set == null) {
                set = new HashSet<>();
                this.handlerFiles.put(messageHandler, set);
            }
            set.add(fileCounter);
            raFile.setLength(msg.getLength());
            System.out.println("Created file " + file.toString() + " of size " + msg.getLength());
            messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter++, false, "OK", msg.getFilename()));
        } catch (IOException e) {
            System.out.println("Cannot access file " + msg.getFilename());
            messageHandler.queueMessage(new SMessageFileMetadataStatus(fileCounter, true, "Cannot access file", msg.getFilename()));
        }
    }

    @Override
    public void messageFileData(CMessageFileData msg, MessageHandler messageHandler) {
        try {
            RandomAccessFile raFile = this.fileIdToFile.get(msg.getFileId());
            if (raFile == null) {
                messageHandler.queueMessage(new SMessageFileDataStatus(msg.getFileId(), msg.getIndex(), true, "No such file"));
                return;
            }
            raFile.seek(msg.getIndex() * Constants.FILE_PIECE_SIZE);
            raFile.write(msg.getData());
            System.out.println("Write " + msg.getLen() + " of data by padding " + msg.getIndex() * Constants.FILE_PIECE_SIZE
                    + " (index " + msg.getIndex() + ")");
            messageHandler.queueMessage(new SMessageFileDataStatus(msg.getFileId(), msg.getIndex(), false, "OK"));
        } catch (IOException e) {
            messageHandler.queueMessage(new SMessageFileDataStatus(msg.getFileId(), msg.getIndex(), true, e.getMessage()));
        }
    }

    @Override
    public void messageFileOk(CMessageFileOk msg, MessageHandler messageHandler) {
        try {
            RandomAccessFile file = this.fileIdToFile.get(msg.getFileId());
            if (file == null) {
                return;
            }
            this.fileIdToFile.get(msg.getFileId()).close();
            this.fileIdToFile.remove(msg.getFileId());
            System.out.println("File " + msg.getFileId() + " closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() throws IOException {
        for (; ; ) {
            int selected = this.selector.select();
            if (selected == 0) {
                continue;
            }
            Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                if (!key.isValid()) {
                    key.cancel();
                }
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = channel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new MessageHandler(this));
                    }
                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        MessageHandler handler = (MessageHandler) key.attachment();
                        handler.read(client);
                    }
                    if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        MessageHandler handler = (MessageHandler) key.attachment();
                        handler.write(client);
                    }
                } catch (ClosedChannelException | CancelledKeyException ex) {
                    System.err.println("Client disconnected");
                    if (key.attachment() instanceof MessageHandler) {
                        this.closeFiles((MessageHandler) key.attachment());
                    }
                    key.cancel();
                } catch (IOException ex) {
                    System.err.println("Lost connection with client");
                    if (key.attachment() instanceof MessageHandler) {
                        this.closeFiles((MessageHandler) key.attachment());
                    }
                    key.cancel();
                }
                selectedKeys.remove();
            }
        }
    }

    private void closeFiles(MessageHandler handler) throws IOException {
        Set<Integer> set = this.handlerFiles.get(handler);
        if (set == null) {
            return;
        }
        for (Integer i : this.handlerFiles.get(handler)) {
            RandomAccessFile file = this.fileIdToFile.get(i);
            if (file != null) {
                file.close();
                System.out.println("File " + i + " closed");
            }
        }
        this.handlerFiles.remove(handler);
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
