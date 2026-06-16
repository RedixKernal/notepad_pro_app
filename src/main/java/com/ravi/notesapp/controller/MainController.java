package com.ravi.notesapp.controller;

import com.ravi.notesapp.app.MainApp;
import com.ravi.notesapp.model.AppSettings;
import com.ravi.notesapp.model.OpenFile;
import com.ravi.notesapp.util.DialogUtils;
import com.ravi.notesapp.util.ThemeUtils;
import com.ravi.notesapp.viewmodel.MainViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import com.ravi.notesapp.util.KeyboardShortcuts;

public class MainController implements Initializable {

    // ── FXML injections ──────────────────────────────────────────────────
    @FXML
    private ToolBar toolBar;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Menu menuRecent;
    @FXML
    private CheckMenuItem menuAutoSave;
    @FXML
    private ToggleButton btnToggleSidebar;
    @FXML
    private javafx.scene.layout.VBox explorerPane;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblFileInfo;

    // Sub-controllers injected by FXML includes
    @FXML
    ExplorerController explorerPaneController;
    @FXML
    EditorController editorPaneController;
    @FXML
    AIChatController aiChatPaneController;

    @FXML
    private ToggleButton btnToggleAi;
    @FXML
    private javafx.scene.layout.VBox aiChatPane;

    // Window dragging offsets
    private double xOffset = 0;
    private double yOffset = 0;

    // Command Palette
    private javafx.stage.Popup palettePopup;
    private PaletteController paletteController;

    // ── ViewModel ─────────────────────────────────────────────────────────
    private final MainViewModel vm = new MainViewModel();

    public MainViewModel getVm() {
        return vm;
    }

    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Wire child controllers
        if (explorerPaneController != null) {
            explorerPaneController.init(vm.getExplorerViewModel(), this);
        }
        editorPaneController.init(vm.getEditorViewModel(), this);
        aiChatPaneController.init(this);

        // Hide sidebar and AI pane initially
        if (explorerPane != null) {
            splitPane.getItems().remove(explorerPane);
        }
        if (aiChatPane != null) {
            splitPane.getItems().remove(aiChatPane);
        }

        // Status bar binding
        lblStatus.textProperty().bind(vm.statusTextProperty());

        vm.getEditorViewModel().activeFileProperty().addListener((obs, oldFile, newFile) -> {
            lblFileInfo.textProperty().unbind();
            if (newFile != null) {
                lblFileInfo.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                        () -> newFile.getFileName() + (newFile.isDirty() ? " •" : ""),
                        newFile.dirtyProperty(), newFile.pathProperty()));
            } else {
                lblFileInfo.setText("");
            }
        });

        // Initialize and listen to recent files changes reactively
        updateRecentMenu();
        Platform.runLater(() -> {
            vm.getEditorViewModel().getRecentFiles().addListener(
                    (javafx.collections.ListChangeListener<com.ravi.notesapp.model.RecentFolder>) c -> updateRecentMenu());
        });

        // Persisted settings
        AppSettings s = vm.getSettings();
        vm.setCurrentTheme(ThemeUtils.getCurrentTheme());

        if (menuAutoSave != null) {
            menuAutoSave.setSelected(s.isAutoSave());
        }

        splitPane.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                // Ensure ThemeUtils currentTheme is set correctly if we want to sync VM
                vm.setCurrentTheme(ThemeUtils.getCurrentTheme());

                // Register shortcuts
                scene.getAccelerators().put(KeyboardShortcuts.TOGGLE_SIDEBAR,
                        () -> Platform.runLater(() -> {
                            btnToggleSidebar.setSelected(!btnToggleSidebar.isSelected());
                            onToggleSidebar(null);
                        }));

                scene.getAccelerators().put(KeyboardShortcuts.SAVE,
                        () -> Platform.runLater(() -> onSave(null)));

                scene.getAccelerators().put(KeyboardShortcuts.OPEN_FOLDER,
                        () -> Platform.runLater(() -> onOpenFolder(null)));

                scene.getAccelerators().put(KeyboardShortcuts.FIND,
                        () -> Platform.runLater(() -> onFind(null)));

                scene.getAccelerators().put(KeyboardShortcuts.TOGGLE_THEME,
                        () -> Platform.runLater(() -> onToggleTheme(null)));

                scene.getAccelerators().put(KeyboardShortcuts.RUN_CODE,
                        () -> Platform.runLater(() -> editorPaneController.runActiveFile()));

                scene.getAccelerators().put(KeyboardShortcuts.COMMAND_PALETTE,
                        () -> Platform.runLater(this::showCommandPalette));

                scene.getAccelerators().put(KeyboardShortcuts.TOGGLE_AI,
                        () -> Platform.runLater(() -> {
                            btnToggleAi.setSelected(!btnToggleAi.isSelected());
                            onToggleAi(null);
                        }));

                scene.getAccelerators().put(KeyboardShortcuts.REFRESH_TREE,
                        () -> Platform.runLater(() -> {
                            if (explorerPaneController != null) {
                                try {
                                    explorerPaneController.getVm().refresh();
                                    vm.setStatusText("Explorer refreshed.");
                                } catch (java.io.IOException ignored) {
                                }
                            }
                        }));

                // Stage close handler
                scene.windowProperty().addListener((wObs, oldWin, win) -> {
                    if (win instanceof Stage stage) {
                        com.ravi.notesapp.util.ResizeHelper.addResizeListener(stage);
                        stage.setOnCloseRequest(e -> {
                            persistSessionState();
                            persistAndExit();
                        });
                    }
                });
            }
        });

        // Restore last open folder and session
        Platform.runLater(() -> {
            if (s.getLastOpenedFolder() != null && explorerPaneController != null) {
                Path last = Path.of(s.getLastOpenedFolder());
                if (java.nio.file.Files.isDirectory(last)) {
                    try {
                        explorerPaneController.openFolder(last);
                    } catch (IOException ignored) {
                    }
                }
            }

            com.ravi.notesapp.model.SessionState session = vm.getSessionService().loadSession();
            for (com.ravi.notesapp.model.SessionTab tab : session.getTabs()) {
                editorPaneController.restoreSessionTab(tab);
            }
        });

        // Command Palette Setup
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/ravi/notesapp/view/palette.fxml"));
            javafx.scene.layout.VBox paletteNode = loader.load();
            paletteController = loader.getController();
            palettePopup = new javafx.stage.Popup();
            palettePopup.getContent().add(paletteNode);
            palettePopup.setAutoHide(true);
            paletteController.init(palettePopup, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showCommandPalette() {
        if (getStage() == null || !getStage().isShowing())
            return;
        String lastFolder = vm.getSettings().getLastOpenedFolder();
        if (lastFolder != null) {
            Path root = Path.of(lastFolder);
            if (java.nio.file.Files.isDirectory(root)) {
                paletteController.show(root);
                double x = getStage().getX() + getStage().getWidth() / 2 - 250;
                double y = getStage().getY() + 100;
                palettePopup.show(getStage(), x, y);
                return;
            }
        }
        DialogUtils.info("Info", "Open a folder first to use Command Palette.");
    }

    @FXML
    void onOpenFolder(ActionEvent e) {
        if (explorerPaneController == null) return;
        DialogUtils.chooseFolder(getStage(), "Open Folder").ifPresent(path -> {
            try {
                explorerPaneController.openFolder(path);
                vm.getSettings().setLastOpenedFolder(path.toAbsolutePath().toString());
                vm.setStatusText("Opened: " + path);
            } catch (IOException ex) {
                DialogUtils.error("Error", "Cannot open folder", ex);
            }
        });
    }

    @FXML
    void onOpenFile(ActionEvent e) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Open File");
        java.io.File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            try {
                editorPaneController.openFile(file.toPath());
            } catch (IOException ex) {
                DialogUtils.error("Error", "Unsupported file type", ex);
            }
        }
    }

    @FXML
    void onSaveAll(ActionEvent e) {
        try {
            for (com.ravi.notesapp.model.OpenFile of : vm.getEditorViewModel().getOpenFiles()) {
                if (of.isDirty()) {
                    if (of.getPath() == null) {
                        vm.getEditorViewModel().setActiveFile(of);
                        onSaveAs(e);
                    } else {
                        vm.getEditorViewModel().save(of);
                    }
                }
            }
            vm.setStatusText("Saved all files.");
        } catch (IOException ex) {
            DialogUtils.error("Save Error", "Could not save all files", ex);
        }
    }

    @FXML
    void onToggleAutoSave(ActionEvent e) {
        boolean isAutoSave = menuAutoSave.isSelected();
        vm.getSettings().setAutoSave(isAutoSave);
        vm.setStatusText("Auto Save " + (isAutoSave ? "Enabled" : "Disabled"));
        if (isAutoSave) {
            onSaveAll(null);
        }
    }

    @FXML
    void onNewFile(ActionEvent e) {
        if (explorerPaneController != null) explorerPaneController.onNewFile(e);
    }

    @FXML
    void onNewFolder(ActionEvent e) {
        if (explorerPaneController != null) explorerPaneController.onNewFolder(e);
    }

    @FXML
    void onRename(ActionEvent e) {
        if (explorerPaneController != null) explorerPaneController.onRename(e);
    }

    @FXML
    void onDelete(ActionEvent e) {
        com.ravi.notesapp.model.OpenFile active = vm.getEditorViewModel().getActiveFile();
        if (active == null) return;
        if (active.getPath() == null) {
            editorPaneController.closeTabByOpenFile(active);
            return;
        }
        if (DialogUtils.confirm("Delete File", null, "Are you sure you want to move " + active.getFileName() + " to the Recycle Bin?")) {
            try {
                boolean trashed = false;
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MOVE_TO_TRASH)) {
                    trashed = java.awt.Desktop.getDesktop().moveToTrash(active.getPath().toFile());
                }
                if (!trashed) {
                    java.nio.file.Files.delete(active.getPath());
                }
                editorPaneController.notifyDeleted(active.getPath());
                vm.setStatusText("Moved to Recycle Bin: " + active.getFileName());
            } catch (Exception ex) {
                DialogUtils.error("Error", "Could not delete file", ex);
            }
        }
    }

    @FXML
    void onSave(ActionEvent e) {
        com.ravi.notesapp.model.OpenFile active = vm.getEditorViewModel().getActiveFile();
        if (active == null) return;
        if (active.getPath() == null) {
            onSaveAs(e);
            return;
        }
        try {
            editorPaneController.saveActive();
            vm.setStatusText("Saved.");
        } catch (IOException ex) {
            DialogUtils.error("Save Error", "Could not save file", ex);
        }
    }

    @FXML
    void onSaveAs(ActionEvent e) {
        OpenFile active = vm.getEditorViewModel().getActiveFile();
        if (active == null)
            return;
        DialogUtils.saveAs(getStage(), "Save As", active.getFileName()).ifPresent(path -> {
            try {
                vm.getEditorViewModel().saveAs(active, path);
                vm.setStatusText("Saved as: " + path.getFileName());
            } catch (IOException ex) {
                DialogUtils.error("Save Error", "Could not save file", ex);
            }
        });
    }

    @FXML
    void onCloseSaved(ActionEvent e) {
        editorPaneController.closeSavedFiles();
        vm.setStatusText("Closed saved files");
    }

    @FXML
    void onExit(ActionEvent e) {
        persistSessionState();
        persistAndExit();
    }

    @FXML
    void onFind(ActionEvent e) {
        editorPaneController.showFind();
    }

    @FXML
    void onFindInFiles(ActionEvent e) {
        editorPaneController.showFindInFiles();
    }

    private double lastDividerPosition = 0.22;

    @FXML
    void onToggleSidebar(ActionEvent e) {
        if (btnToggleSidebar == null || splitPane == null || explorerPane == null)
            return;
        boolean visible = btnToggleSidebar.isSelected();
        if (visible) {
            if (!splitPane.getItems().contains(explorerPane)) {
                splitPane.getItems().add(0, explorerPane);
                splitPane.setDividerPosition(0, lastDividerPosition);
            }
        } else {
            if (splitPane.getItems().contains(explorerPane)) {
                if (splitPane.getDividerPositions().length > 0) {
                    lastDividerPosition = splitPane.getDividerPositions()[0];
                }
                splitPane.getItems().remove(explorerPane);
            }
        }
    }

    private double lastAiDividerPosition = 0.75;

    @FXML
    void onToggleAi(ActionEvent e) {
        if (btnToggleAi == null || splitPane == null || aiChatPane == null)
            return;
        boolean visible = btnToggleAi.isSelected();
        if (visible) {
            if (!splitPane.getItems().contains(aiChatPane)) {
                splitPane.getItems().add(aiChatPane);
                splitPane.setDividerPosition(splitPane.getDividers().size() - 1, lastAiDividerPosition);
            }
        } else {
            if (splitPane.getItems().contains(aiChatPane)) {
                if (splitPane.getDividers().size() > 0) {
                    lastAiDividerPosition = splitPane.getDividerPositions()[splitPane.getDividers().size() - 1];
                }
                splitPane.getItems().remove(aiChatPane);
            }
        }
    }

    @FXML
    void onRunActiveFile(ActionEvent e) {
        if (editorPaneController != null) {
            editorPaneController.runActiveFile();
        }
    }

    @FXML
    void onToggleTheme(ActionEvent event) {
        ThemeUtils.toggleTheme(splitPane.getScene());
        boolean isDark = ThemeUtils.isDark();
        vm.setCurrentTheme(isDark ? "dark" : "light");
        vm.setStatusText("Theme: " + ThemeUtils.getCurrentTheme());
        if (aiChatPaneController != null) {
            aiChatPaneController.updateTheme(isDark);
        }
    }

    @FXML
    void onAbout(ActionEvent e) {
        DialogUtils.info("About Notepad_Pro",
                "Notepad_Pro v2.0.0\nA modern file explorer\nBuilt with modern technology\n\n Design and Developed by Redix Systems");
    }

    public void notifyFolderOpened(Path folder) {
        vm.setStatusText("Opened: " + folder);
        vm.getSettings().setLastOpenedFolder(folder.toAbsolutePath().toString());
    }

    private void persistSessionState() {
        com.ravi.notesapp.model.SessionState state = new com.ravi.notesapp.model.SessionState();
        java.util.List<com.ravi.notesapp.model.SessionTab> tabs = new java.util.ArrayList<>();
        for (com.ravi.notesapp.model.OpenFile of : vm.getEditorViewModel().getOpenFiles()) {
            String path = of.getPath() != null ? of.getPath().toAbsolutePath().toString() : null;
            tabs.add(new com.ravi.notesapp.model.SessionTab(path, of.getContent(), of.isDirty()));
        }
        state.setTabs(tabs);
        com.ravi.notesapp.model.OpenFile active = vm.getEditorViewModel().getActiveFile();
        if (active != null && active.getPath() != null) {
            state.setActiveTabPath(active.getPath().toAbsolutePath().toString());
        }
        vm.getSessionService().saveSession(state);
    }

    private void persistAndExit() {
        double dividerPos = lastDividerPosition;
        if (splitPane.getItems().contains(explorerPane) && splitPane.getDividerPositions().length > 0) {
            dividerPos = splitPane.getDividerPositions()[0];
        }
        vm.persistSettings(
                getStage().getWidth(),
                getStage().getHeight(),
                dividerPos);
        Platform.exit();
        System.exit(0);
    }

    private Stage getStage() {
        if (splitPane != null && splitPane.getScene() != null) {
            return (Stage) splitPane.getScene().getWindow();
        }
        return MainApp.primaryStage;
    }

    private void updateRecentMenu() {
        if (menuRecent == null)
            return;
        menuRecent.getItems().clear();

        if (vm.getEditorViewModel() != null) {
            for (com.ravi.notesapp.model.RecentFolder rf : vm.getEditorViewModel().getRecentFiles()) {
                MenuItem item = new MenuItem(rf.toPath().toString());
                item.setOnAction(e -> {
                    try {
                        editorPaneController.openFile(rf.toPath());
                    } catch (IOException ex) {
                        DialogUtils.error("Error", "Could not open file", ex);
                    }
                });
                menuRecent.getItems().add(item);
            }
        }

        if (menuRecent.getItems().isEmpty()) {
            MenuItem empty = new MenuItem("No Recent Files");
            empty.setDisable(true);
            menuRecent.getItems().add(empty);
        } else {
            menuRecent.getItems().add(new SeparatorMenuItem());
            MenuItem clearItem = new MenuItem("Clear Recent Files");
            clearItem.setOnAction(e -> vm.getEditorViewModel().clearRecentFiles());
            menuRecent.getItems().add(clearItem);
        }
    }

    public void openFileFromArgs(String filePath) {
        if (filePath == null || filePath.isBlank())
            return;
        Platform.runLater(() -> {
            try {
                Path path = Path.of(filePath);
                if (java.nio.file.Files.isRegularFile(path)) {
                    editorPaneController.openFile(path);
                }
            } catch (Exception e) {
                System.err.println("Failed to open file from command line args: " + e.getMessage());
            }
        });
    }
}
