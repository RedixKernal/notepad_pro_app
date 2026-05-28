package com.ravi.notesapp.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ravi.notesapp.model.RecentFolder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists recently opened folders to ~/.notesapp/recent.json
 */
public class RecentService {

    private static final int MAX_RECENT = 10;
    private static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".notesapp");
    private static final Path RECENT_FILE = DATA_DIR.resolve("recent.json");
    private static final Path RECENT_FILES_FILE = DATA_DIR.resolve("recent_files.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<RecentFolder> recentFolders = new ArrayList<>();
    private List<RecentFolder> recentFiles = new ArrayList<>();

    public RecentService() {
        load();
        loadFiles();
    }

    public void addRecent(Path folder) {
        String absPath = folder.toAbsolutePath().toString();
        recentFolders.removeIf(r -> r.getAbsolutePath().equals(absPath));
        recentFolders.add(0, new RecentFolder(folder));
        if (recentFolders.size() > MAX_RECENT) {
            recentFolders = recentFolders.subList(0, MAX_RECENT);
        }
        save();
    }

    public List<RecentFolder> getRecentFolders() {
        return recentFolders.stream()
                .filter(r -> Files.isDirectory(r.toPath()))
                .sorted(Comparator.comparingLong(RecentFolder::getLastOpened).reversed())
                .collect(Collectors.toList());
    }

    public void removeRecent(Path folder) {
        String absPath = folder.toAbsolutePath().toString();
        recentFolders.removeIf(r -> r.getAbsolutePath().equals(absPath));
        save();
    }

    public void clearRecent() {
        recentFolders.clear();
        save();
    }

    public void addRecentFile(Path file) {
        String absPath = file.toAbsolutePath().toString();
        recentFiles.removeIf(r -> r.getAbsolutePath().equals(absPath));
        recentFiles.add(0, new RecentFolder(file));
        if (recentFiles.size() > MAX_RECENT) {
            recentFiles = recentFiles.subList(0, MAX_RECENT);
        }
        saveFiles();
    }

    public List<RecentFolder> getRecentFiles() {
        return recentFiles.stream()
                .filter(r -> Files.isRegularFile(r.toPath()))
                .sorted(Comparator.comparingLong(RecentFolder::getLastOpened).reversed())
                .collect(Collectors.toList());
    }

    public void removeRecentFile(Path file) {
        String absPath = file.toAbsolutePath().toString();
        recentFiles.removeIf(r -> r.getAbsolutePath().equals(absPath));
        saveFiles();
    }

    public void clearRecentFiles() {
        recentFiles.clear();
        saveFiles();
    }

    // ---------- persistence ----------
    private void load() {
        try {
            if (!Files.exists(RECENT_FILE)) return;
            String json = Files.readString(RECENT_FILE, StandardCharsets.UTF_8);
            Type type = new TypeToken<List<RecentFolder>>() {}.getType();
            List<RecentFolder> loaded = gson.fromJson(json, type);
            if (loaded != null) recentFolders = loaded;
        } catch (IOException e) {
            recentFolders = new ArrayList<>();
        }
    }

    private void save() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(RECENT_FILE, gson.toJson(recentFolders), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private void loadFiles() {
        try {
            if (!Files.exists(RECENT_FILES_FILE)) return;
            String json = Files.readString(RECENT_FILES_FILE, StandardCharsets.UTF_8);
            Type type = new TypeToken<List<RecentFolder>>() {}.getType();
            List<RecentFolder> loaded = gson.fromJson(json, type);
            if (loaded != null) recentFiles = loaded;
        } catch (IOException e) {
            recentFiles = new ArrayList<>();
        }
    }

    private void saveFiles() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(RECENT_FILES_FILE, gson.toJson(recentFiles), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }
}
