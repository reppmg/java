package server;

import common.FileUploadTask;
import com.sun.media.sound.InvalidFormatException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    public static final String serverName = "System";

    private static final Logger log = Logger.getGlobal();
    private static final int BUFFER_SIZE = 1024;


    private static void logDebug(String data) {
        log.log(Level.INFO, data);
    }

    public static final int PORT = 1234;
    private final Selector selector;
    /**
     * active clients
     */
    private final Set<SelectionKey> clients = new HashSet<>();
    /**
     * message history
     */
    private final ArrayList<Message> history = new ArrayList<>();


    public static void main(String[] args) {
        try {
            new Server().run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Server() throws IOException {
        logDebug("Init started");
        selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        logDebug("Init finished");
    }

    private void run() throws IOException {
        while (true) {
            int readyCount = selector.select();
            if (readyCount == 0) {
//                sendPendingMessages();
                continue;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();


                if (!key.isValid()) {
                    continue;
                }
                processKey(key);
                iterator.remove();
            }
        }
    }

    private void processKey(SelectionKey key) {
        if (key.isAcceptable()) {
            acceptConnection(key);
        } else if (key.isReadable()) {
            readMessage(key);
        }
    }

    /**
     * sends message to client
     * @param key client's key
     * @param message message to be sent
     */
    private void sendMessage(SelectionKey key, Message message) {
        String text;
        if (message instanceof TextMessage)
            text = ((TextMessage) message).getFullText() + "\n";
        else if (message instanceof FileMessage)
            text = ((FileMessage) message).getMessageText() + "\n";
        else
            return;
        SocketChannel channel = (SocketChannel) key.channel();
        logDebug("sending message " + text + " to " + channel.toString());
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.put(text.getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error sending message");
        }
    }

    /**
     * registers client
     * @param key clients {@link SelectionKey}
     */
    private void acceptConnection(SelectionKey key) {
        logDebug("accepting a connection");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            if (client == null) return;
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            SelectionKey clientKey = client.keyFor(selector);
            clients.add(clientKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads message from key
     * @param key client's {@link SelectionKey}
     */
    private void readMessage(SelectionKey key) {
        logDebug("reading message from " + key.attachment());
        int BUFFER_SIZE = 1024;
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            int numReadBytes = client.read(buffer);
            if (shouldCloseConnection(numReadBytes)) {
                client.close();
                channelDisconnected(key);
                return;
            }
            if (key.attachment() instanceof FileSaver) {
                FileSaver fileSaver = (FileSaver) key.attachment();
                logDebug("Continue saving file " + fileSaver.getPath());
                fileSaver.save(buffer);
            } else {
                String text = new String(buffer.array());
                logDebug(text.substring(0, Math.min(100, text.length() - 1)));
                Message message = parseMessage(text, key);
                if (message instanceof TextMessage) {
                    history.add(message);
                    broadcastExcept(key, (TextMessage) message);
                } else if (message instanceof LoginMessage) {
                    broadcastMessage(((LoginMessage) message).buildSystemNotification());
                } else if (message instanceof HistoryMessage) {
                    sendHistory(key);
                } else if (message instanceof FileMessage) {
                    FileMessage fileMessage = (FileMessage) message;
                    if (fileMessage.isUploadRequest()) {
                        initFileSaving(key, fileMessage);
                    } else {
                        logDebug("Uploading file " + fileMessage.getId());
                        new Thread(new FileUploadTask(client, fileMessage.getFilePath(), serverName)).start();
                    }
                }
            }
        } catch (InvalidFormatException | NumberFormatException e) {
            logDebug("malformed file save command");
            sendInvalidFormatError(key);
        } catch (FileNotFoundException e) {
            logDebug("file not found");
            sendNoFileError(key);
        } catch (IOException e) {
            e.printStackTrace();
            cleanupClientConnection(key, client);
        }
    }

    private void sendNoFileError(SelectionKey key) {
        sendMessage(key, new TextMessage("no such file in system", serverName));
    }

    private void sendInvalidFormatError(SelectionKey key) {
        sendMessage(key, new TextMessage("Invalid format of command", serverName));
    }

    /**
     * initiates file upload from client
     * @param key client
     * @param fileMessage file command wrapper
     * @throws IOException file is unavailable
     */
    private void initFileSaving(SelectionKey key, FileMessage fileMessage) throws IOException {
        FileSaver fileSaver = new FileSaver(fileMessage.getFileName());
        key.attach(fileSaver);
        fileSaver.setMessage(fileMessage);
        Path savedFilePath = fileSaver.getPath();
        fileMessage.setFilePath(savedFilePath);
        logDebug("File will be saved " + savedFilePath.toAbsolutePath());
    }

    /**
     * Broadcasts message to all logged in clients
     * @param text message to be sent
     */
    private void broadcast(String text) {
        clients.stream()
                .filter((key -> key.attachment() instanceof String))
                .forEach((client) -> sendMessage(client, new TextMessage(text, serverName)));
    }

    /**
     * Broadcasts message to all logged in clients
     * @param textMessage message to be sent
     */
    private void broadcastMessage(TextMessage textMessage) {
        clients.stream()
                .filter((key -> key.attachment() instanceof String))
                .forEach((client) -> sendMessage(client, textMessage));
    }

    /**
     * closes connections after client disconnected
     * @param key client's {@link SelectionKey}
     * @param client key's channel
     */
    private void cleanupClientConnection(SelectionKey key, SocketChannel client) {
        try {
            clients.remove(key);
            channelDisconnected(key);
            client.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * broadcasts disconnect message or finishes file upload
     * @param key disconnected one
     */
    private void channelDisconnected(SelectionKey key) {
        if (key.attachment() instanceof String)
            broadcastExcept(key, new TextMessage(key.attachment() + " disconnected", serverName));
        if (key.attachment() instanceof FileSaver) {
            FileSaver fileSaver = (FileSaver) key.attachment();
            FileMessage fileMessage = fileSaver.getMessage();
            fileSaver.finish();
            broadcast(String.format("%s uploaded file %d", fileMessage.getAuthor(), fileMessage.getId()));
            history.add(fileMessage);
        }
    }

    /**
     * sends message history
     * @param key client
     */
    private void sendHistory(SelectionKey key) {
        history.forEach((message -> sendMessage(key, message)));
    }

    /**
     * parses message
     * @param text raw message
     * @param key client
     * @return message wrapper object
     * @throws InvalidFormatException when command has invalid format
     * @throws FileNotFoundException when file command contains file that not exists
     */
    private Message parseMessage(String text, SelectionKey key) throws InvalidFormatException, FileNotFoundException {
        if (text.startsWith("/login")) {
            LoginMessage message = LoginMessage.parse(text);
            key.attach(message.author);
            return message;
        } else if (text.startsWith("/history")) {
            return new HistoryMessage(((String) key.attachment()));
        } else if (text.startsWith("/file")) {
            return FileMessage.parse(text, (String) key.attachment());
        } else if (text.startsWith("/download")) {
            return FileMessage.parseDownloadRequest(text, serverName);
        } else {
            return new TextMessage(text, ((String) key.attachment()));
        }
    }

    /**
     * Broadcasts message to all logged clients except one
     * @param key exception client
     * @param textMessage message to be sent
     */
    private void broadcastExcept(SelectionKey key, TextMessage textMessage) {
        clients.stream()
                .filter((key1 -> key1.attachment() instanceof String))
                .forEach((client) -> {
                    if (client != key) {
                        sendMessage(client, textMessage);
                    }
                });
    }

    /**
     * defines should the channel be closed
     * @param numReadBytes read bytes from the channel
     * @return should close this channel
     */
    private boolean shouldCloseConnection(int numReadBytes) {
        boolean remoteClientClosedConnection = numReadBytes == -1;
        if (remoteClientClosedConnection) {
            logDebug("Client closed connection");
            return true;
        }
        return false;
    }
}
