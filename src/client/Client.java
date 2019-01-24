package client;

import common.FileUploadTask;
import server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client class for the app
 */
public class Client {
    /**
     * restricted to use in login symbols
     */
    private static final String[] RESTRICTED = new String[]{" ", "\"", ";", "'", "/"};

    private static final int BUFFER_SIZE = 1024;
    private final InetSocketAddress remote = new InetSocketAddress(Server.PORT);

    private final SocketChannel channel;
    private String name;

    public static void main(String[] args) throws IOException {
        new Client().run();
    }

    private Client() throws IOException {
        InetSocketAddress remote = this.remote;
        channel = SocketChannel.open(remote);
        channel.configureBlocking(false);
    }

    /**
     * Runs the app
     *
     * @throws IOException if connection to server is impossible
     */
    private void run() throws IOException {
        System.out.print("Enter your name: ");
        Scanner scanner = new Scanner(System.in);
        name = scanner.nextLine();
        while (Arrays.stream(RESTRICTED).anyMatch(name::contains)) {
            System.out.println("Try again. You cannot use those symbols: " + Arrays.toString(RESTRICTED));
            name = scanner.nextLine();
        }
        login();
        SocketChannelWatcher socketChannelWatcher = new SocketChannelWatcher(channel);
        socketChannelWatcher.watchAsync();
        while (true) {
            String message = scanner.nextLine();
            if (message.startsWith("/file")) {
                startFileSequence();
            } else if (message.startsWith("/download")) {
                requestFile(message);
            } else if (message.startsWith("/help")) {
                printHelp();
            } else {
                sendMessage(message);
            }
        }
    }

    /**
     * prints help on commands
     */
    private void printHelp() {
        System.out.println("Enter a command or message. Command list:");
        System.out.println("/file - upload file");
        System.out.println("/download {file id} - downloads file by id");
        System.out.println("/history - prints message history");
        System.out.println("/online - prints users online");
    }

    /**
     * requests file download from server
     *
     * @param message with /file command
     * @throws IOException when server is unavailable
     */
    private void requestFile(String message) throws IOException {
        SocketChannel fileSocket = SocketChannel.open(remote);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.put(message.getBytes());
        buffer.flip();
        fileSocket.write(buffer);
        new FileDownloader(fileSocket).start();
    }

    /**
     * starts upload file to server
     *
     * @throws IOException when server is unavailable
     */
    private void startFileSequence() throws IOException {
        List<Path> files = FileWatcher.getFiles();
        int index = 1;
        System.out.println("Chose file (0 for exit):");
        for (Path file : files) {
            System.out.println(String.format("%d: %s", index++, file.toAbsolutePath()));
        }
        Scanner scanner = new Scanner(System.in);
        int fileNum = -2;
        while (fileNum < 0 || fileNum >= files.size()){
            try {
                fileNum = scanner.nextInt() - 1;
                if (fileNum == -1) {
                    System.out.println("Exiting file menu");
                    return;
                }
                if (fileNum < 0 || fileNum >= files.size()) {
                    System.out.println("Cannot file with this index. (0 for exit)");
                }
            } catch (Exception e) {
                System.out.println("Error! Enter number. (0 for exit)");
                if (scanner.hasNext()) scanner.nextLine();
            }
        }

        Path selectedFilePath = files.get(fileNum);
        SocketChannel fileSocket = SocketChannel.open(remote);
        new Thread(new FileUploadTask(fileSocket, selectedFilePath, name)).start();
        System.out.println("upload started: " + selectedFilePath.toAbsolutePath());
    }

    /**
     * @param message to be sent to server
     */
    private void sendMessage(String message) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.put(message.getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error sending message");
        }
    }

    /**
     * sends login command
     */
    private void login() {
        sendMessage("/login " + name);
    }

}
