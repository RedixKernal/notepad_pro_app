package com.ravi.notesapp.util;

import java.nio.file.Path;
import java.util.Locale;

/**
 * File extension helpers and display utilities.
 */
public final class FileUtils {

    private FileUtils() {}

    /** Map a file path to an emoji icon for display. */
    public static String getIcon(Path path, boolean isDirectory) {
        if (isDirectory) return "📁";
        String name = path.getFileName() != null ? path.getFileName().toString().toLowerCase(Locale.ROOT) : "";
        if (name.endsWith(".java"))       return "☕";
        if (name.endsWith(".kt"))         return "🟣";
        if (name.endsWith(".py"))         return "🐍";
        if (name.endsWith(".js"))         return "🟨";
        if (name.endsWith(".ts"))         return "🔷";
        if (name.endsWith(".html"))       return "🌐";
        if (name.endsWith(".css"))        return "🎨";
        if (name.endsWith(".json"))       return "📋";
        if (name.endsWith(".xml"))        return "📄";
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return "⚙️";
        if (name.endsWith(".md"))         return "📝";
        if (name.endsWith(".txt"))        return "📃";
        if (name.endsWith(".sh"))         return "🖥️";
        if (name.endsWith(".sql"))        return "🗄️";
        if (name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".gif") || name.endsWith(".svg")) return "🖼️";
        if (name.endsWith(".pdf"))        return "📕";
        if (name.endsWith(".zip") || name.endsWith(".tar")
                || name.endsWith(".gz"))  return "🗜️";
        if (name.endsWith(".gradle") || name.endsWith(".kts")) return "🐘";
        return "📄";
    }

    /** Human-readable file size. */
    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    /** Get file extension (lowercase, without dot). */
    public static String getExtension(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** Return true if the extension suggests a binary file. */
    public static boolean isBinary(Path path) {
        String ext = getExtension(path);
        return switch (ext) {
            case "png", "jpg", "jpeg", "gif", "bmp", "ico",
                 "pdf", "zip", "jar", "class", "exe", "dll",
                 "tar", "gz", "7z", "rar" -> true;
            default -> false;
        };
    }
}
