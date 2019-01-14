package server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to save files from opened SocketChannel
 */
public class FileSaver {
    private Path destinationPath;
    private final OutputStream outputStream;
    private FileMessage message;

    /**
     *
     * @param fileName destination file name
     * @throws IOException if the destination file is not writable
     */
    public FileSaver(String fileName) throws IOException {
        destinationPath = Paths.get(fileName);
        tryDeleteExisting(destinationPath);
        outputStream = Files.newOutputStream(destinationPath);
    }

    /**
     *
     * @param buffer containing file's byte data
     */
    public void save(ByteBuffer buffer) {
        try {
            outputStream.write(buffer.array());
            outputStream.flush();
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * Must be called when all data received from socket
     */
    public void finish() {
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void tryDeleteExisting(Path destinationPath) {
        try {
            Files.delete(destinationPath);
        } catch (IOException e) {
            //ignore
        }
    }

    public Path getPath() {
        return destinationPath;
    }

    public void setMessage(FileMessage message) {
        this.message = message;
    }

    public FileMessage getMessage() {
        return message;
    }
}
