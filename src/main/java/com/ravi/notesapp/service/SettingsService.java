package com.ravi.notesapp.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ravi.notesapp.model.AppSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Persists AppSettings to ~/.notesapp/settings.json
 */
public class SettingsService {

    private static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".notesapp");
    private static final Path SETTINGS_FILE = DATA_DIR.resolve("settings.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private AppSettings settings;

    public SettingsService() {
        settings = load();
    }

    public AppSettings getSettings() {
        return settings;
    }

    public void save() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(SETTINGS_FILE, gson.toJson(settings), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private AppSettings load() {
        try {
            if (!Files.exists(SETTINGS_FILE)) return new AppSettings();
            String json = Files.readString(SETTINGS_FILE, StandardCharsets.UTF_8);
            AppSettings loaded = gson.fromJson(json, AppSettings.class);
            return loaded != null ? loaded : new AppSettings();
        } catch (IOException e) {
            return new AppSettings();
        }
    }
}
