package com.ravi.notesapp.viewmodel;

import com.ravi.notesapp.model.AppSettings;
import com.ravi.notesapp.model.OpenFile;
import com.ravi.notesapp.service.*;
import javafx.beans.property.*;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Top-level ViewModel wiring services and child ViewModels.
 */
public class MainViewModel {

    // Services (singletons for this app)
    private final FileService    fileService    = new FileService();
    private final EditorService  editorService  = new EditorService(fileService);
    private final SearchService  searchService  = new SearchService();
    private final RecentService  recentService  = new RecentService();
    private final SettingsService settingsService = new SettingsService();
    private final SessionService sessionService = new SessionService();

    // Child VMs
    private final ExplorerViewModel explorerViewModel;
    private final EditorViewModel   editorViewModel;

    // UI state
    private final StringProperty  statusText     = new SimpleStringProperty("Ready");
    private final StringProperty  currentTheme   = new SimpleStringProperty("dark");

    public MainViewModel() {
        explorerViewModel = new ExplorerViewModel(fileService, recentService);
        editorViewModel   = new EditorViewModel(editorService, fileService, recentService);
    }

    // ---------- Accessors ----------
    public ExplorerViewModel getExplorerViewModel() { return explorerViewModel; }
    public EditorViewModel   getEditorViewModel()   { return editorViewModel; }
    public AppSettings       getSettings()          { return settingsService.getSettings(); }
    public SettingsService   getSettingsService()   { return settingsService; }
    public SessionService    getSessionService()    { return sessionService; }
    public RecentService     getRecentService()     { return recentService; }

    public StringProperty statusTextProperty() { return statusText; }
    public String getStatusText() { return statusText.get(); }
    public void setStatusText(String text) { statusText.set(text); }

    public StringProperty currentThemeProperty() { return currentTheme; }
    public void setCurrentTheme(String theme) { currentTheme.set(theme); }

    // ---------- Global save helpers ----------
    public void saveAll() throws IOException {
        editorService.saveAll();
        setStatusText("All files saved.");
    }

    public boolean hasDirtyFiles() {
        return editorService.hasDirtyFiles();
    }

    public void persistSettings(double w, double h, double divider) {
        AppSettings s = settingsService.getSettings();
        s.setWindowWidth(w);
        s.setWindowHeight(h);
        s.setDividerPosition(divider);
        s.setTheme(currentTheme.get());
        settingsService.save();
    }
}
