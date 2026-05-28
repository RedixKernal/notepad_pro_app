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

    // Sub-controllers injected by FXML includes
    @FXML
    ExplorerController explorerPaneController;
    @FXML
    EditorController editorPaneController;

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
        explorerPaneController.init(vm.getExplorerViewModel(), this);
        editorPaneController.init(vm.getEditorViewModel(), this);

        // Initialize and listen to recent folders changes reactively
        updateRecentMenu();
        explorerPaneController.getVm().getRecentFolders().addListener(
                (javafx.collections.ListChangeListener<com.ravi.notesapp.model.RecentFolder>) c -> updateRecentMenu());

        // Active file status (no longer updating footer labels)
        vm.getEditorViewModel().activeFileProperty().addListener((obs, o, file) -> {
            // Can be used for future UI updates
        });

        // Persisted settings
        AppSettings s = vm.getSettings();
        vm.setCurrentTheme(s.getTheme());
        
        if (menuAutoSave != null) {
            menuAutoSave.setSelected(s.isAutoSave());
        }

        splitPane.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                // KeyboardShortcuts.setup(scene, this, editorPaneController);
                ThemeUtils.applyTheme(scene, s.getTheme());

                // Register Ctrl+B shortcut to toggle sidebar
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.B,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                btnToggleSidebar.setSelected(!btnToggleSidebar.isSelected());
                                onToggleSidebar(null);
                            });
                        });

                // Register Ctrl+S shortcut to save file
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                onSave(null);
                            });
                        });

                // Register Ctrl+O shortcut to open folder
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                onOpenFolder(null);
                            });
                        });

                // Register Ctrl+F shortcut to find in file
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                onFind(null);
                            });
                        });

                // Register Ctrl+Shift+T shortcut to toggle theme
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.T,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN,
                                javafx.scene.input.KeyCombination.SHIFT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                onToggleTheme(null);
                            });
                        });

                // Register Ctrl+R shortcut to run code
                scene.getAccelerators().put(
                        new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.R,
                                javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                        () -> {
                            javafx.application.Platform.runLater(() -> {
                                editorPaneController.runActiveFile();
                            });
                        });

                // Stage close handler — stage is available by now
                scene.windowProperty().addListener((wObs, oldWin, win) -> {
                    if (win instanceof javafx.stage.Stage stage) {
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
            if (s.getLastOpenedFolder() != null) {
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

                // Position popup centered
                double x = getStage().getX() + getStage().getWidth() / 2 - 250;
                double y = getStage().getY() + 100;
                palettePopup.show(getStage(), x, y);
            }
        }
    }

    // ──────────────── Window Controls ────────────────────────────────────

    // Custom window controls have been removed.

    // ──────────────── File Menu Handlers ─────────────────────────────────

    @FXML
    void onOpenFolder(ActionEvent e) {
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
            vm.saveAll();
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
        
        // If enabled, save all currently unsaved files
        if (isAutoSave) {
            onSaveAll(null);
        }
    }

    @FXML
    void onNewFile(ActionEvent e) {
        explorerPaneController.onNewFile(e);
    }

    @FXML
    void onNewFolder(ActionEvent e) {
        explorerPaneController.onNewFolder(e);
    }

    @FXML
    void onRename(ActionEvent e) {
        explorerPaneController.onRename(e);
    }

    @FXML
    void onDelete(ActionEvent e) {
        explorerPaneController.onDelete(e);
    }

    @FXML
    void onSave(ActionEvent e) {
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

    // ──────────────── Edit / Search ──────────────────────────────────────

    @FXML
    void onFind(ActionEvent e) {
        editorPaneController.showFind();
    }

    @FXML
    void onFindInFiles(ActionEvent e) {
        editorPaneController.showFindInFiles();
    }

    // ──────────────── View ───────────────────────────────────────────────

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

    @FXML
    void onToggleTheme(ActionEvent event) {
        ThemeUtils.toggleTheme(splitPane.getScene());
        vm.setCurrentTheme(ThemeUtils.isDark() ? "dark" : "light");
        vm.setStatusText("Theme: " + ThemeUtils.getCurrentTheme());
    }

    // ──────────────── Help ───────────────────────────────────────────────

    @FXML
    void onAbout(ActionEvent e) {
        DialogUtils.info("About Notepad_Pro",
                "Notepad_Pro v0.1.0\nA modern file explorer\nBuilt with modern technology\n\n Design and Developed by Redix Systems");
    }

    // ──────────────── Internal helpers ───────────────────────────────────

    /** Called by ExplorerController when a folder is opened. */
    public void notifyFolderOpened(Path folder) {
        vm.setStatusText("Opened: " + folder);
        vm.getSettings().setLastOpenedFolder(folder.toAbsolutePath().toString());
    }

    private void handleAppExit() {
        boolean allSaved = true;
        for (OpenFile of : vm.getEditorViewModel().getOpenFiles()) {
            if (of.isDirty()) {
                DialogUtils.UnsavedChoice choice = DialogUtils.unsavedChanges(of.getFileName());
                if (choice == DialogUtils.UnsavedChoice.CANCEL)
                    return;
                if (choice == DialogUtils.UnsavedChoice.SAVE) {
                    try {
                        vm.getEditorViewModel().saveActive();
                    } catch (IOException ex) {
                        allSaved = false;
                    }
                }
            }
        }
        if (allSaved) {
            persistAndExit();
        }
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
        for (com.ravi.notesapp.model.RecentFolder rf : explorerPaneController.getVm().getRecentFolders()) {
            MenuItem item = new MenuItem(rf.toPath().toString());
            item.setOnAction(evt -> {
                try {
                    explorerPaneController.openFolder(rf.toPath());
                } catch (IOException ex) {
                    DialogUtils.error("Error", "Cannot open folder", ex);
                }
            });
            menuRecent.getItems().add(item);
        }
        if (menuRecent.getItems().isEmpty()) {
            MenuItem empty = new MenuItem("No Recent Folders");
            empty.setDisable(true);
            menuRecent.getItems().add(empty);
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
