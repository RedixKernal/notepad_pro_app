package com.ravi.notesapp.viewmodel;

import com.ravi.notesapp.model.OpenFile;
import com.ravi.notesapp.service.EditorService;
import com.ravi.notesapp.service.FileService;
import com.ravi.notesapp.service.RecentService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * ViewModel for the right-panel tabbed editor.
 */
public class EditorViewModel {

    private final EditorService editorService;
    private final FileService   fileService;
    private final RecentService recentService;

    private final ObservableList<OpenFile>  openFiles      = FXCollections.observableArrayList();
    private final ObjectProperty<OpenFile>  activeFile     = new SimpleObjectProperty<>();
    private final StringProperty            searchQuery    = new SimpleStringProperty("");
    private final BooleanProperty           searchVisible  = new SimpleBooleanProperty(false);

    public EditorViewModel(EditorService editorService, FileService fileService, RecentService recentService) {
        this.editorService = editorService;
        this.fileService   = fileService;
        this.recentService = recentService;
    }

    // ---------- Open ----------
    public OpenFile openFile(Path path) throws IOException {
        Optional<OpenFile> existing = editorService.getOpenFile(path);
        if (existing.isPresent()) {
            activeFile.set(existing.get());
            recentService.addRecentFile(path);
            return existing.get();
        }
        OpenFile of = editorService.openFile(path);
        openFiles.add(of);
        activeFile.set(of);
        recentService.addRecentFile(path);
        return of;
    }

    public OpenFile restoreSessionFile(Path path, String content, boolean isDirty) {
        OpenFile of = new OpenFile(path, content);
        of.setDirty(isDirty);
        editorService.restoreOpenFile(of);
        openFiles.add(of);
        return of;
    }

    // ---------- Close ----------
    public boolean closeFile(OpenFile of) {
        editorService.closeFile(of.getPath());
        openFiles.remove(of);
        if (activeFile.get() == of) {
            activeFile.set(openFiles.isEmpty() ? null : openFiles.get(openFiles.size() - 1));
        }
        return true;
    }

    // ---------- Save ----------
    public void saveActive() throws IOException {
        OpenFile of = activeFile.get();
        if (of != null) editorService.saveFile(of);
    }

    public void saveAs(OpenFile of, Path newPath) throws IOException {
        editorService.saveFileAs(of, newPath);
        recentService.addRecentFile(newPath);
    }

    public void saveAll() throws IOException {
        editorService.saveAll();
    }

    // ---------- Update path after rename ----------
    public void updatePath(Path oldPath, Path newPath) {
        editorService.updatePath(oldPath, newPath);
    }

    // ---------- Content changed ----------
    public void onContentChanged(OpenFile of, String newContent) {
        of.setContent(newContent);
        of.setDirty(!newContent.equals(of.getSavedContent()));
    }

    // ---------- Search ----------
    public void showSearch()  { searchVisible.set(true); }
    public void hideSearch()  { searchVisible.set(false); }
    public void toggleSearch(){ searchVisible.set(!searchVisible.get()); }

    // ---------- Properties ----------
    public ObservableList<OpenFile>  getOpenFiles()          { return openFiles; }
    public ObjectProperty<OpenFile>  activeFileProperty()    { return activeFile; }
    public OpenFile                  getActiveFile()          { return activeFile.get(); }
    public void                      setActiveFile(OpenFile f){ activeFile.set(f); }
    public StringProperty            searchQueryProperty()   { return searchQuery; }
    public BooleanProperty           searchVisibleProperty() { return searchVisible; }
}
