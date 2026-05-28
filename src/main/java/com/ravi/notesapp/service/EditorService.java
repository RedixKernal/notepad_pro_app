package com.ravi.notesapp.service;

import com.ravi.notesapp.model.OpenFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages the collection of open editor tabs.
 * Keeps track of dirty state, open/close lifecycle.
 */
public class EditorService {

    private final FileService fileService;
    // Ordered map preserving tab insertion order
    private final LinkedHashMap<Path, OpenFile> openFiles = new LinkedHashMap<>();

    public EditorService(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Open a file. If already open, return the existing OpenFile.
     */
    public OpenFile openFile(Path path) throws IOException {
        if (openFiles.containsKey(path)) {
            return openFiles.get(path);
        }
        String content = fileService.readFile(path);
        OpenFile openFile = new OpenFile(path, content);
        openFiles.put(path, openFile);
        return openFile;
    }

    public void restoreOpenFile(OpenFile openFile) {
        if (openFile.getPath() != null) {
            openFiles.put(openFile.getPath(), openFile);
        }
    }

    /**
     * Close a tab (removes from tracking).
     */
    public void closeFile(Path path) {
        openFiles.remove(path);
    }

    /**
     * Save the current content of an open file to disk.
     */
    public void saveFile(OpenFile openFile) throws IOException {
        fileService.saveFile(openFile.getPath(), openFile.getContent());
        openFile.markSaved();
    }

    /**
     * Save as — write to new path, update OpenFile's path.
     */
    public void saveFileAs(OpenFile openFile, Path newPath) throws IOException {
        openFiles.remove(openFile.getPath());
        fileService.saveFile(newPath, openFile.getContent());
        openFile.setPath(newPath);
        openFile.markSaved();
        openFiles.put(newPath, openFile);
    }

    /**
     * Save all dirty open files.
     */
    public void saveAll() throws IOException {
        for (OpenFile f : openFiles.values()) {
            if (f.isDirty()) {
                fileService.saveFile(f.getPath(), f.getContent());
                f.markSaved();
            }
        }
    }

    public boolean isOpen(Path path) {
        return openFiles.containsKey(path);
    }

    public Optional<OpenFile> getOpenFile(Path path) {
        return Optional.ofNullable(openFiles.get(path));
    }

    public Collection<OpenFile> getOpenFiles() {
        return Collections.unmodifiableCollection(openFiles.values());
    }

    /** Update path key when a file is renamed. */
    public void updatePath(Path oldPath, Path newPath) {
        OpenFile of = openFiles.remove(oldPath);
        if (of != null) {
            of.setPath(newPath);
            openFiles.put(newPath, of);
        }
    }

    public boolean hasDirtyFiles() {
        return openFiles.values().stream().anyMatch(OpenFile::isDirty);
    }
}
