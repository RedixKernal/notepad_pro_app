package com.ravi.notesapp.model;

import javafx.beans.property.*;

import java.nio.file.Path;

/**
 * Represents a currently opened file in the editor.
 * dirty = unsaved changes.
 */
public class OpenFile {
    private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private final StringProperty content = new SimpleStringProperty("");
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final StringProperty savedContent = new SimpleStringProperty("");

    public OpenFile(Path path, String content) {
        this.path.set(path);
        this.content.set(content);
        this.savedContent.set(content);
    }

    // ---------- Path ----------
    public Path getPath() { return path.get(); }
    public ObjectProperty<Path> pathProperty() { return path; }
    public void setPath(Path path) { this.path.set(path); }

    // ---------- Content ----------
    public String getContent() { return content.get(); }
    public StringProperty contentProperty() { return content; }
    public void setContent(String content) { this.content.set(content); }

    // ---------- Dirty ----------
    public boolean isDirty() { return dirty.get(); }
    public BooleanProperty dirtyProperty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty.set(dirty); }

    // ---------- Saved snapshot ----------
    public String getSavedContent() { return savedContent.get(); }
    public void markSaved() {
        savedContent.set(content.get());
        dirty.set(false);
    }

    public String getFileName() {
        Path p = path.get();
        return p != null && p.getFileName() != null ? p.getFileName().toString() : "Untitled";
    }
}
