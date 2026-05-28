package com.ravi.notesapp.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Represents a recently opened folder entry.
 */
public class RecentFolder {
    private String absolutePath;
    private long lastOpened; // epoch millis

    public RecentFolder() {} // for Gson

    public RecentFolder(Path path) {
        this.absolutePath = path.toAbsolutePath().toString();
        this.lastOpened = Instant.now().toEpochMilli();
    }

    public String getAbsolutePath() { return absolutePath; }
    public void setAbsolutePath(String absolutePath) { this.absolutePath = absolutePath; }

    public long getLastOpened() { return lastOpened; }
    public void setLastOpened(long lastOpened) { this.lastOpened = lastOpened; }

    public Path toPath() {
        return Path.of(absolutePath);
    }

    public String getDisplayName() {
        return Path.of(absolutePath).getFileName() != null
                ? Path.of(absolutePath).getFileName().toString()
                : absolutePath;
    }
}
