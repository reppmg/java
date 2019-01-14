package server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * static class contain info about users' files
 */
public class FileUtils {
    static int filesCount = 0;
    static List<String> files = new ArrayList<>();
    static Map<Integer, Path> filePaths = new HashMap<>();
}
