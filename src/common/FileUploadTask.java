package common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


/**
 * Class to upload files to open SocketChannel
 */
public class FileUploadTask implements Runnable {
    public static final int BUFFER_SIZE = 1024;
    private final SocketChannel channel;
    private final Path filePath;
    private final String name;

    /**
     *
     * @param channel to send file
     * @param fileName source file name
     * @param name client's name
     */
    public FileUploadTask(SocketChannel channel, Path fileName, String name) {
        this.channel = channel;
        this.filePath = fileName;
        this.name = name;
    }

    @Override
    public void run() {
        sendMeta();
        if (Files.exists(filePath)) {
            try (InputStream inputStream = Files.newInputStream(filePath);
            SocketChannel channel = this.channel) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                while (inputStream.read(buffer.array()) != -1) {
                    channel.write(buffer);
                    buffer.clear();
                    Arrays.fill(buffer.array(), (byte) 0);
                }
                channel.close();
            } catch (IOException e) {
                System.out.println("Cannot upload this file. Try another one");
            }
        }
    }

    /**
     * sends command's meta data (command, file name, user's name)
     */
    private void sendMeta() {
        String metaInfo = String.format("/file \"%s\"%s\n", filePath.getFileName().toString(), name);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.put(metaInfo.getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
