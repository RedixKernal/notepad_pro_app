package com.ravi.notesapp.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Provides file-name search and in-file text search capabilities.
 */
public class SearchService {

    /**
     * Search for files whose name contains {@code query} (case-insensitive)
     * under {@code root}, up to {@code maxResults}.
     */
    public List<Path> searchFiles(Path root, String query, int maxResults) throws IOException {
        if (root == null || query == null || query.isBlank()) return List.of();
        String lq = query.toLowerCase(Locale.ROOT);
        List<Path> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(lq))
                .limit(maxResults)
                .forEach(results::add);
        }
        return results;
    }

    /**
     * Search for lines containing {@code query} inside {@code file}.
     * Returns a list of (lineNumber, lineContent) pairs.
     */
    public List<SearchResult> searchInFile(Path file, String query, boolean caseSensitive) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        List<SearchResult> results = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);
        String searchQuery = caseSensitive ? query : query.toLowerCase(Locale.ROOT);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String searchLine = caseSensitive ? line : line.toLowerCase(Locale.ROOT);
            if (searchLine.contains(searchQuery)) {
                results.add(new SearchResult(i + 1, line.trim(), file));
            }
        }
        return results;
    }

    /**
     * Search for text across all files in a directory.
     */
    public List<SearchResult> searchInDirectory(Path root, String query, boolean caseSensitive, int maxResults)
            throws IOException {
        if (root == null || query == null || query.isBlank()) return List.of();
        List<SearchResult> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> isTextFile(p.getFileName().toString()))
                .forEach(file -> {
                    if (results.size() >= maxResults) return;
                    try {
                        List<SearchResult> fileResults = searchInFile(file, query, caseSensitive);
                        results.addAll(fileResults.subList(0, Math.min(fileResults.size(), maxResults - results.size())));
                    } catch (IOException ignored) {}
                });
        }
        return results;
    }

    private boolean isTextFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md")  || lower.endsWith(".java") ||
               lower.endsWith(".json")|| lower.endsWith(".xml") || lower.endsWith(".yaml") ||
               lower.endsWith(".yml") || lower.endsWith(".properties") || lower.endsWith(".csv") ||
               lower.endsWith(".html")|| lower.endsWith(".css") || lower.endsWith(".js")   ||
               lower.endsWith(".ts")  || lower.endsWith(".py")  || lower.endsWith(".sh")   ||
               lower.endsWith(".kt")  || lower.endsWith(".gradle") || lower.endsWith(".sql");
    }

    // ------------------------------------------------------------------ //
    public record SearchResult(int lineNumber, String lineContent, Path file) {
        public String getDisplayPath(Path root) {
            return root != null ? root.relativize(file).toString() : file.toString();
        }
    }
}
