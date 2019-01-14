package server;

import com.sun.media.sound.InvalidFormatException;

import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * wrapper for file messages from/to client
 * command syntax is: /file "filename"userName
 */
public class FileMessage extends Message {
    /**
     * file id. File is accessed by clients by id
     */
    private int id = -1;

    private final String fileName;
    /**
     * defines if file should be downloaded or uploaded
     */
    private final boolean uploadRequest;
    /**
     * file message contains meta info (command, file name, user name). Length of it in bytes
     */
    private int metaInfoLength;
    private Path filePath;


    /**
     *
     * @param author client's name
     * @param fileName destination file name
     * @param uploadRequest {@link #uploadRequest}
     * @param id {@link #id}
     */
    public FileMessage(String author, String fileName, boolean uploadRequest, int id) {
        super(author);
        this.fileName = fileName;
        this.uploadRequest = uploadRequest;
        this.id = id;
    }

    /**
     * Parses object from raw text
     * @param text source text
     * @param author client's name
     * @return {@link FileMessage} object
     * @throws InvalidFormatException when command has invalid format
     */
    public static FileMessage parse(String text, String author) throws InvalidFormatException {
        if (text.length() < 7) throw new InvalidFormatException(author);
        int firstQuoteIndex = text.indexOf("\"");
        boolean uploadRequest = firstQuoteIndex != -1;
        String fileName;
        int id;
        int metaInfoLength = 0;
        String authorFromMessage = "";
        if (uploadRequest) {
            int secondQuoteIndex = text.indexOf("\"", firstQuoteIndex + 1);
            int metaEndIndex = text.indexOf("\n");
            fileName = text.substring(firstQuoteIndex + 1, secondQuoteIndex);
            id = FileUtils.filesCount++;
            FileUtils.files.add(fileName);
            metaInfoLength = text.substring(0, metaEndIndex + 1).getBytes().length;
            authorFromMessage = text.substring(secondQuoteIndex + 1, metaEndIndex);
        } else {
            String indexStr = text.substring(text.indexOf(" ") + 1, text.indexOf(";"));
            int index = Integer.parseInt(indexStr);
            fileName = FileUtils.files.get(index);
            id = index;
        }
        FileMessage fileMessage = new FileMessage(author == null ? authorFromMessage : author, fileName, uploadRequest, id);
        fileMessage.metaInfoLength = metaInfoLength;
        return fileMessage;
    }

    /**
     * Parses object from download request (/download fileId)
     * @param text command text
     * @param author client's name
     * @return {@link FileMessage} object
     * @throws FileNotFoundException when file with specified id is not present
     */
    public static FileMessage parseDownloadRequest(String text, String author) throws FileNotFoundException {
        String indexStr = text.substring(text.indexOf(" ") + 1);
        Integer index = Integer.parseInt(indexStr.trim());
        if (index < 0 || index > FileUtils.filesCount) {
            throw new FileNotFoundException(author);
        }
        Path filePath = FileUtils.filePaths.get(index);
        String fileName = FileUtils.files.get(index);
        FileMessage fileMessage = new FileMessage(author, fileName, false, index);
        fileMessage.setFilePath(filePath);
        return fileMessage;
    }

    /**
     *
     * @return file name for saving on server: author_fileName
     */
    public String getFileName() {
        return String.format("%s_%s", author, fileName);
    }

    public String getCleanFileName() {
        return fileName;
    }

    public boolean isUploadRequest() {
        return uploadRequest;
    }

    public int getMetaInfoLength() {
        return metaInfoLength;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
        FileUtils.filePaths.put(id, filePath);
    }

    public Path getFilePath() {
        return filePath;
    }

    public static FileMessage parse(String message) throws InvalidFormatException {
        return parse(message, Server.serverName);
    }

    public int getId() {
        return id;
    }

    /**
     *
     * @return text for history command
     */
    public String getMessageText(){
        return String.format("%s: %d", getFileName(), id);
    }
}
