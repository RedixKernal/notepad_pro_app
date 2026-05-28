package com.ravi.notesapp.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ravi.notesapp.model.SessionState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class SessionService {

    private static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".notesapp");
    private static final Path SESSION_FILE = DATA_DIR.resolve("session.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void saveSession(SessionState state) {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(SESSION_FILE, gson.toJson(state), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    public SessionState loadSession() {
        try {
            if (!Files.exists(SESSION_FILE)) return new SessionState();
            String json = Files.readString(SESSION_FILE, StandardCharsets.UTF_8);
            SessionState loaded = gson.fromJson(json, SessionState.class);
            return loaded != null ? loaded : new SessionState();
        } catch (IOException e) {
            return new SessionState();
        }
    }
}
