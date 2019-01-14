package client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileWatcher {

    private static List<Path> files;

    /**
     *
     * @return list of files in current dir and subdirectories
     */
    public static List<Path> getFiles() {
        files = new ArrayList<>();
        traversePath(Paths.get(""));
        return files;
    }

    private static void traversePath(Path path) {
        boolean isDirectory = Files.isDirectory(path);
        if (isDirectory) {
            try {
                Files.list(path).forEach(FileWatcher::traversePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            files.add(path);
        }
    }
}
