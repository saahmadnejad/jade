package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared virtual file tree holding all Artifacts one team instance
 * produces. Optionally mirrors every file to a real directory so the team's
 * work product can be opened in an IDE.
 */
public class Workspace {

    private final String id;
    private final Map<String, String> files = new ConcurrentHashMap<>();
    private final Path mirrorDir;

    public Workspace(String id, Path mirrorDir) {
        this.id = id;
        this.mirrorDir = mirrorDir;
    }

    public String getId() {
        return id;
    }

    public void save(String path, String content) {
        if (path == null || path.isBlank()) {
            return;
        }
        String normalized = normalize(path);
        files.put(normalized, content);
        if (mirrorDir != null) {
            try {
                Path target = mirrorDir.resolve(normalized).normalize();
                if (!target.startsWith(mirrorDir)) {
                    return; // refuse path traversal
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, content);
            } catch (IOException e) {
                System.err.println("[Workspace:" + id + "] disk mirror failed for "
                    + normalized + ": " + e.getMessage());
            }
        }
    }

    public String get(String path) {
        return files.get(normalize(path));
    }

    /** Copy of all files, insertion order preserved. */
    public Map<String, String> snapshot() {
        return new LinkedHashMap<>(files);
    }

    private static String normalize(String path) {
        String p = path.trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }
}
