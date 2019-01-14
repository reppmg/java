package client;

import server.FileMessage;
import server.FileSaver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import static common.FileUploadTask.BUFFER_SIZE;

public class FileDownloader implements Runnable {
    private final SocketChannel channel;
    private FileSaver fileSaver = null;
    private final Selector selector;

    public FileDownloader(SocketChannel channel) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (true) {
            try {
                int numReadBytes = channel.read(buffer);
                boolean noMessage = numReadBytes == 0;
                if (noMessage) {
                    continue;
                }
                if (numReadBytes == -1) {
                    closeFileChannel(channel);
                    break;
                }
                if (fileSaver != null) {
                    fileSaver.save(buffer);
                } else {
                    String message = new String(buffer.array(), StandardCharsets.UTF_8);
                    if (message.startsWith("/file")) {
                        System.out.println("File is being saved");
                        FileMessage fileMessage = FileMessage.parse(message);
                        fileSaver = new FileSaver(fileMessage.getCleanFileName());
                        Path path = fileSaver.getPath();
                        if (buffer.position() > fileMessage.getMetaInfoLength()) {
                            ByteBuffer fileDataBuffer = ByteBuffer.allocate(buffer.position() - fileMessage.getMetaInfoLength());
                            int offset = fileMessage.getMetaInfoLength();
                            for (int i = offset; i < buffer.position(); i++) {
                                fileDataBuffer.array()[i - offset] = buffer.get(i);
                            }
                            fileSaver.save(fileDataBuffer);
                        }
                        System.out.println("File will be saved: " + path.toAbsolutePath());
                    } else {
                        System.out.println(message);
                    }
                }
                Arrays.fill(buffer.array(), (byte) 0);
                buffer.clear();

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }

    private void closeFileChannel(SocketChannel channel) throws IOException {
        fileSaver.finish();
        System.out.println("File download complete");
        channel.close();
    }
}
