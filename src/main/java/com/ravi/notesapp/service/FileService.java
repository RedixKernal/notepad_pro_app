package com.ravi.notesapp.service;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles all NIO file-system operations.
 * UI-independent — never touches JavaFX nodes.
 */
public class FileService {

    /** Read entire file as UTF-8 string. */
    public String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /** Write string to file as UTF-8. */
    public void saveFile(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * List immediate children of a directory, sorted:
     * directories first, then files, both alphabetically.
     */
    public List<Path> listChildren(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.sorted(Comparator
                    .comparing((Path p) -> !Files.isDirectory(p))
                    .thenComparing(p -> p.getFileName().toString().toLowerCase())
            ).collect(Collectors.toList());
        }
    }

    /** Create a new empty file. */
    public void createFile(Path file) throws IOException {
        Files.createFile(file);
    }

    /** Create all necessary directories. */
    public void createDirectory(Path dir) throws IOException {
        Files.createDirectories(dir);
    }

    /** Rename / move a path to a new name in the same parent. */
    public Path rename(Path oldPath, String newName) throws IOException {
        Path newPath = oldPath.resolveSibling(newName);
        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        return newPath;
    }

    /**
     * Move file or directory to the system Recycle Bin / Trash.
     * Returns {@code true} if successfully sent to Recycle Bin.
     * Falls back to permanent delete when platform support is unavailable.
     */
    public boolean delete(Path path) throws IOException {
        java.io.File file = path.toFile();
        // Prefer moving to Recycle Bin when platform supports it
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            boolean moved = Desktop.getDesktop().moveToTrash(file);
            if (moved) return true;
            // moveToTrash returned false — fall through to permanent delete
        }
        // Permanent delete fallback (directories need recursive walk)
        if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                List<Path> sorted = walk.sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());
                for (Path p : sorted) Files.delete(p);
            }
        } else {
            Files.deleteIfExists(path);
        }
        return false;
    }

    /** Copy file to target parent directory. */
    public void copyFile(Path source, Path targetParent) throws IOException {
        Path target = targetParent.resolve(source.getFileName());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public long fileSize(Path path) {
        try { return Files.size(path); } catch (IOException e) { return 0; }
    }
}
