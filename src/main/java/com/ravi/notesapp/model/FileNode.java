package com.ravi.notesapp.model;

import java.nio.file.Path;

/**
 * Represents a single node (file or directory) in the explorer tree.
 */
public class FileNode {
    private final Path path;
    private final boolean directory;

    public FileNode(Path path, boolean directory) {
        this.path = path;
        this.directory = directory;
    }

    public Path getPath() { return path; }
    public boolean isDirectory() { return directory; }

    @Override
    public String toString() {
        return path.getFileName() != null ? path.getFileName().toString() : path.toString();
    }
}
