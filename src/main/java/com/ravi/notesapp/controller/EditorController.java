package com.ravi.notesapp.controller;

import com.ravi.notesapp.model.OpenFile;
import com.ravi.notesapp.service.SearchService;
import com.ravi.notesapp.util.DialogUtils;
import com.ravi.notesapp.util.FileUtils;
import com.ravi.notesapp.viewmodel.EditorViewModel;
import com.ravi.notesapp.viewmodel.ExplorerViewModel;
import com.ravi.notesapp.service.ExecutionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import com.ravi.notesapp.util.ThemeUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class EditorController implements Initializable {

    @FXML
    private TabPane tabPane;
    @FXML
    private StackPane welcomePane;
    @FXML
    private SplitPane editorSplitPane;
    @FXML
    private HBox findBar;
    @FXML
    private TextField findField;
    @FXML
    private CheckBox chkCaseSensitive;
    @FXML
    private Label lblMatchCount;
    @FXML
    private VBox consolePane;
    @FXML
    private HBox consoleHeader;
    @FXML
    private TextArea consoleOutput;

    private double consoleDragInitialDivider;
    private double consoleDragStartY;

    @FXML
    private TextField consoleInput;
    private Process activeProcess;

    private EditorViewModel vm;
    private MainController mainController;
    private final SearchService searchService = new SearchService();
    private final ExecutionService executionService = new ExecutionService();

    // Map from tab to its OpenFile
    private final Map<Tab, OpenFile> tabFileMap = new LinkedHashMap<>();
    private final Map<Path, Tab> pathTabMap = new HashMap<>();

    @FXML
    private ScrollPane tabScrollPane;
    @FXML
    private HBox tabContainer;
    @FXML
    private ListView<String> recentFilesListView;
    private final Map<Tab, Node> nativeToCustomTab = new HashMap<>();
    private Tab addTabPlaceholder;

    // In-file search state
    private List<int[]> matches = new ArrayList<>(); // [start, end]
    private int matchIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remove console pane from split pane initially so it doesn't take up space
        editorSplitPane.getItems().remove(consolePane);
    }

    public void init(EditorViewModel vm, MainController mainController) {
        this.vm = vm;
        this.mainController = mainController;

        boolean initialHasTabs = !tabPane.getTabs().isEmpty();
        tabScrollPane.setVisible(initialHasTabs);
        tabScrollPane.setManaged(initialHasTabs);

        tabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> c) -> {
            boolean hasTabs = !tabPane.getTabs().isEmpty();
            tabScrollPane.setVisible(hasTabs);
            tabScrollPane.setManaged(hasTabs);

            while (c.next()) {
                if (c.wasAdded()) {
                    for (Tab t : c.getAddedSubList()) {
                        Node customTab = createCustomTab(t);
                        nativeToCustomTab.put(t, customTab);
                        tabContainer.getChildren().add(customTab);
                    }
                }
                if (c.wasRemoved()) {
                    for (Tab t : c.getRemoved()) {
                        Node customTab = nativeToCustomTab.remove(t);
                        if (customTab != null) {
                            tabContainer.getChildren().remove(customTab);
                        }
                    }
                }
            }
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab != null && nativeToCustomTab.containsKey(oldTab)) {
                nativeToCustomTab.get(oldTab).getStyleClass().remove("selected");
            }
            if (newTab != null && nativeToCustomTab.containsKey(newTab)) {
                Node custom = nativeToCustomTab.get(newTab);
                if (!custom.getStyleClass().contains("selected")) {
                    custom.getStyleClass().add("selected");
                }
                Platform.runLater(() -> ensureVisible(custom));
            }

            if (newTab == null) {
                vm.setActiveFile(null);
                showWelcome(true);
            } else {
                OpenFile of = tabFileMap.get(newTab);
                if (of != null) {
                    vm.setActiveFile(of);
                    showWelcome(false);
                }
            }
        });

        // Forward vertical scroll on the scroll pane to horizontal scroll
        tabScrollPane.setOnScroll(event -> {
            if (event.getDeltaY() != 0) {
                double hvalue = tabScrollPane.getHvalue();
                double width = tabScrollPane.getContent().getBoundsInLocal().getWidth();
                double viewportWidth = tabScrollPane.getViewportBounds().getWidth();
                if (width > viewportWidth) {
                    double delta = event.getDeltaY() > 0 ? -0.1 : 0.1;
                    tabScrollPane.setHvalue(Math.max(0, Math.min(1, hvalue + delta)));
                    event.consume();
                }
            }
        });

        // Recent Files List
        if (recentFilesListView != null) {
            vm.getRecentFiles().addListener(
                    (javafx.collections.ListChangeListener<com.ravi.notesapp.model.RecentFolder>) c -> refreshRecentFilesList());
            refreshRecentFilesList();

            recentFilesListView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    int idx = recentFilesListView.getSelectionModel().getSelectedIndex();
                    if (idx >= 0 && idx < vm.getRecentFiles().size()) {
                        com.ravi.notesapp.model.RecentFolder rf = vm.getRecentFiles().get(idx);
                        try {
                            openFile(rf.toPath());
                        } catch (IOException ex) {
                            com.ravi.notesapp.util.DialogUtils.error("Error", "Could not open file", ex);
                        }
                    }
                }
            });
        }

        showWelcome(true);
    }

    private void refreshRecentFilesList() {
        if (recentFilesListView != null) {
            java.util.List<com.ravi.notesapp.model.RecentFolder> files = vm.getRecentFiles();
            recentFilesListView.getItems().setAll(
                    files.stream().map(rf -> "📄  " + rf.getDisplayName()).toList());
        }
    }

    @FXML
    void onOpenFileFromWelcome(javafx.event.ActionEvent e) {
        if (mainController != null) {
            mainController.onOpenFile(e);
        }
    }

    @FXML
    private Button btnAddTab;

    @FXML
    void onAddTab(ActionEvent e) {
        createNewUntitledTab();
    }

    public void createNewUntitledTab() {
        OpenFile of = vm.createUntitledFile();
        Tab tab = createTab(of);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        showWelcome(false);
    }

    private Node createCustomTab(Tab nativeTab) {
        HBox box = new HBox(8);
        box.getStyleClass().add("custom-tab");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label label = new Label();
        label.textProperty().bind(nativeTab.textProperty());

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("close-btn");
        javafx.scene.shape.SVGPath closeIcon = new javafx.scene.shape.SVGPath();
        closeIcon.setContent(
                "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        closeIcon.getStyleClass().add("svg-icon");
        closeIcon.setScaleX(0.7);
        closeIcon.setScaleY(0.7);
        closeBtn.setGraphic(closeIcon);

        closeBtn.setOnAction(e -> {
            e.consume();
            requestClose(nativeTab);
        });

        box.getChildren().addAll(label, closeBtn);

        box.setOnMouseClicked(e -> {
            tabPane.getSelectionModel().select(nativeTab);
        });

        return box;
    }

    private void ensureVisible(Node node) {
        double width = tabScrollPane.getContent().getBoundsInLocal().getWidth();
        double x = node.getBoundsInParent().getMinX();
        double nodeWidth = node.getBoundsInLocal().getWidth();
        double viewportWidth = tabScrollPane.getViewportBounds().getWidth();

        if (width > viewportWidth) {
            double hvalue = tabScrollPane.getHvalue();
            double visibleLeft = hvalue * (width - viewportWidth);
            double visibleRight = visibleLeft + viewportWidth;

            if (x < visibleLeft) {
                tabScrollPane.setHvalue(x / (width - viewportWidth));
            } else if (x + nodeWidth > visibleRight) {
                tabScrollPane.setHvalue((x + nodeWidth - viewportWidth) / (width - viewportWidth));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Open / Close tabs
    // ─────────────────────────────────────────────────────────────────────

    public void openFile(Path path) throws IOException {
        if (pathTabMap.containsKey(path)) {
            tabPane.getSelectionModel().select(pathTabMap.get(path));
            return;
        }
        OpenFile of = vm.openFile(path);
        Tab tab = createTab(of);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        showWelcome(false);
    }

    private Tab createTab(OpenFile of) {
        Tab tab = new Tab();
        tab.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            String name = (of.getPath() != null && of.getPath().getFileName() != null)
                    ? of.getPath().getFileName().toString()
                    : "Untitled";
            return of.isDirty() ? "● " + name : name;
        }, of.pathProperty(), of.dirtyProperty()));

        // Editor area
        CodeArea area = new CodeArea(of.getContent());
        area.setWrapText(true);
        area.getStyleClass().add("editor-area");


        // Context menu to dynamically change text color
        ContextMenu contextMenu = new ContextMenu();
        MenuItem runCodeItem = new MenuItem("Run Code");
        MenuItem spaceItem = new MenuItem("Space");
        MenuItem copyItem = new MenuItem("Copy");
        MenuItem cutItem = new MenuItem("Cut");
        MenuItem pasteItem = new MenuItem("Paste");
        MenuItem changeColorItem = new MenuItem("Change Text Color");

        copyItem.setOnAction(evt -> area.copy());
        cutItem.setOnAction(evt -> area.cut());
        pasteItem.setOnAction(evt -> area.paste());

        spaceItem.setOnAction(evt -> {
            Dialog<Integer> dialog = new Dialog<>();
            dialog.setTitle("Tab Spaces");
            dialog.setHeaderText(null);

            ButtonType applyBtn = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(applyBtn, ButtonType.CANCEL);

            ComboBox<Integer> comboBox = new ComboBox<>();
            comboBox.getStyleClass().add("no-arrow-combo");
            for (int i = 1; i <= 10; i++) {
                comboBox.getItems().add(i);
            }
            int currentSpace = mainController.getVm().getSettings().getTabSpace();
            comboBox.setValue(currentSpace);
            comboBox.setMaxWidth(Double.MAX_VALUE);

            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
            vbox.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
            Label label = new Label("Tab / Indentation Space");
            label.getStyleClass().add("dialog-prompt-label");
            vbox.getChildren().addAll(label, comboBox);

            dialog.getDialogPane().setContent(vbox);
            dialog.getDialogPane().setPrefWidth(450);

            DialogUtils.styleDialog(dialog);

            dialog.setResultConverter(btn -> {
                if (btn == applyBtn)
                    return comboBox.getValue();
                return null;
            });

            dialog.showAndWait().ifPresent(val -> {
                mainController.getVm().getSettings().setTabSpace(val);
            });
        });

        final double[] fontSize = { 13.0 };
        final String[] textColorHex = { ThemeUtils.isDark() ? "#cccccc" : "#1a2040" };

        Runnable updateEditorStyle = () -> {
            area.setStyle("-fx-font-size: " + fontSize[0] + "px; -editor-text-color: " + textColorHex[0] + ";");
        };

        runCodeItem.setOnAction(evt -> runActiveFile());
        changeColorItem.setOnAction(evt -> {
            // ── Theme colours resolved once ──────────────────────────────────
            boolean dark = ThemeUtils.isDark();
            String textPrimary = dark ? "#cccccc" : "#1a2040";
            String textMuted = dark ? "rgba(200,200,200,0.65)" : "rgba(26,32,64,0.65)";
            String bgBase = dark ? "#1e1e1e" : "#ffffff";
            String bgPanel = dark ? "#252526" : "#f4f5fb";
            String borderColor = dark ? "#3c3c3c" : "#d0d4e8";

            // ── Build dialog content ─────────────────────────────────────────
            Dialog<javafx.util.Pair<Color, Boolean>> dialog = new Dialog<>();
            dialog.setTitle("Text Color");
            dialog.setHeaderText(null);

            ButtonType applyButton = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(applyButton, ButtonType.CANCEL);

            // ── Color picker (full width, taller) ───────────────────────────
            ColorPicker colorPicker = new ColorPicker(Color.web(textColorHex[0]));
            colorPicker.setMaxWidth(Double.MAX_VALUE);
            colorPicker.setPrefHeight(42);
            colorPicker.getStyleClass().add("button");

            // ── Section label: Color ─────────────────────────────────────────
            Label colorLabel = new Label("Editor Text Color");
            colorLabel.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 13px;" +
                            "-fx-text-fill: " + textMuted + ";");

            javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
            sep1.setStyle("-fx-background-color: " + borderColor + ";");

            // ── Apply scope: radio buttons ───────────────────────────────────
            Label scopeLabel = new Label("Apply to");
            scopeLabel.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 13px;" +
                            "-fx-text-fill: " + textMuted + ";");

            ToggleGroup scopeGroup = new ToggleGroup();

            String rbStyle = "-fx-text-fill: " + textPrimary + "; -fx-font-size: 13px;";

            RadioButton rbCurrent = new RadioButton("Current tab only");
            rbCurrent.setToggleGroup(scopeGroup);
            rbCurrent.setSelected(true);
            rbCurrent.setMaxWidth(Double.MAX_VALUE);
            rbCurrent.setStyle(rbStyle);

            RadioButton rbAll = new RadioButton("All open tabs");
            rbAll.setToggleGroup(scopeGroup);
            rbAll.setMaxWidth(Double.MAX_VALUE);
            rbAll.setStyle(rbStyle);

            javafx.scene.layout.VBox radioBox = new javafx.scene.layout.VBox(10, rbCurrent, rbAll);
            radioBox.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

            // ── Assemble layout ──────────────────────────────────────────────
            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(12,
                    colorLabel, colorPicker,
                    sep1,
                    scopeLabel, radioBox);
            vbox.setPadding(new javafx.geometry.Insets(20, 22, 12, 22));
            vbox.setPrefWidth(340);
            vbox.setStyle("-fx-background-color: " + bgBase + ";");
            dialog.getDialogPane().setContent(vbox);
            dialog.getDialogPane().setPrefWidth(384);
            dialog.getDialogPane().setStyle(
                    "-fx-background-color: " + bgPanel + ";" +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: 1;");

            // ── Apply theme stylesheet ───────────────────────────────────────
            String css = dark
                    ? "/com/ravi/notesapp/styles/dark.css"
                    : "/com/ravi/notesapp/styles/light.css";
            java.net.URL resource = EditorController.class.getResource(css);
            if (resource != null) {
                dialog.getDialogPane().getStylesheets().add(resource.toExternalForm());
            }

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == applyButton) {
                    return new javafx.util.Pair<>(colorPicker.getValue(), rbAll.isSelected());
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                Color color = result.getKey();
                boolean applyToAll = result.getValue();
                String hex = String.format("#%02x%02x%02x",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));

                if (applyToAll) {
                    // Apply to every open tab's CodeArea
                    for (Tab t : tabPane.getTabs()) {
                        if (t.getContent() instanceof VirtualizedScrollPane) {
                            Object content = ((VirtualizedScrollPane<?>) t.getContent()).getContent();
                            if (content instanceof CodeArea ca) {
                                ca.setStyle(ca.getStyle()
                                        .replaceAll("-editor-text-color:\\s*[^;]+;", "")
                                        + " -editor-text-color: " + hex + ";");
                            }
                        }
                    }
                    // Keep this tab's state in sync too
                    textColorHex[0] = hex;
                    updateEditorStyle.run();
                } else {
                    textColorHex[0] = hex;
                    updateEditorStyle.run();
                }
            });
        });

        contextMenu.getItems().addAll(runCodeItem, spaceItem, copyItem, cutItem, pasteItem, changeColorItem);
        area.setContextMenu(contextMenu);

        // Bind Ctrl + Plus/Minus key events to change font size, and handle Tab / Enter
        area.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.isControlDown()) {
                if (evt.getCode() == KeyCode.PLUS || evt.getCode() == KeyCode.EQUALS || evt.getCode() == KeyCode.ADD
                        || "+".equals(evt.getText()) || "=".equals(evt.getText())) {
                    fontSize[0] += 1.0;
                    updateEditorStyle.run();
                    evt.consume();
                } else if (evt.getCode() == KeyCode.MINUS || evt.getCode() == KeyCode.SUBTRACT
                        || "-".equals(evt.getText())) {
                    if (fontSize[0] > 8.0) {
                        fontSize[0] -= 1.0;
                        updateEditorStyle.run();
                        evt.consume();
                    }
                }
            } else if (evt.getCode() == KeyCode.TAB) {
                int spaces = mainController.getVm().getSettings().getTabSpace();
                String spaceStr = " ".repeat(spaces);
                area.replaceSelection(spaceStr);
                evt.consume();
            } else if (evt.getCode() == KeyCode.ENTER) {
                String currentLine = area.getParagraph(area.getCurrentParagraph()).getText();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s+").matcher(currentLine);
                String indent = m.find() ? m.group() : "";
                area.replaceSelection("\n" + indent);
                evt.consume();
            }
        });

        // Ensure caret is at the beginning, so view starts from the first position
        area.moveTo(0);
        area.requestFollowCaret();

        // Initial highlight
        if (of.getPath() != null) {
            String ext = FileUtils.getExtension(of.getPath());
            // area.setStyleSpans(0, SyntaxHighlighter.computeHighlighting(area.getText(),
            // ext));
        }

        // Sync content changes
        javafx.animation.PauseTransition autoSaveTimer = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1));
        autoSaveTimer.setOnFinished(evt -> {
            if (mainController.getVm().getSettings().isAutoSave() && of.isDirty()) {
                try {
                    vm.saveAll();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        area.textProperty().addListener((obs, oldText, newText) -> {
            vm.onContentChanged(of, newText);
            if (of.getPath() != null) {
                String ext = FileUtils.getExtension(of.getPath());
                // area.setStyleSpans(0, SyntaxHighlighter.computeHighlighting(newText, ext));
            }
            if (mainController.getVm().getSettings().isAutoSave()) {
                autoSaveTimer.playFromStart();
            }
        });

        // Wrap CodeArea in VirtualizedScrollPane to show scroll bars!
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(area);
        tab.setContent(scrollPane);
        tabFileMap.put(tab, of);
        pathTabMap.put(of.getPath(), tab);

        // Update path mapping if file is saved-as
        of.pathProperty().addListener((obs, oldPath, newPath) -> {
            if (oldPath != null)
                pathTabMap.remove(oldPath);
            if (newPath != null)
                pathTabMap.put(newPath, tab);
        });

        // Close request
        tab.setOnCloseRequest(e -> {
            e.consume();
            requestClose(tab);
        });

        tab.setOnClosed(e -> {
            OpenFile closed = tabFileMap.remove(tab);
            if (closed != null) {
                if (closed.getPath() != null)
                    pathTabMap.remove(closed.getPath());
                vm.closeFile(closed);
            }
            if (tabPane.getTabs().isEmpty())
                showWelcome(true);
        });

        return tab;
    }

    private void requestClose(Tab tab) {
        OpenFile of = tabFileMap.get(tab);
        if (of != null && of.isDirty()) {
            DialogUtils.UnsavedChoice choice = DialogUtils.unsavedChanges(of.getFileName());
            if (choice == DialogUtils.UnsavedChoice.CANCEL) {
                return;
            }
            if (choice == DialogUtils.UnsavedChoice.SAVE) {
                try {
                    // Make it active so saveActive triggers the correct save flow (including Save
                    // As dialog if untitled)
                    tabPane.getSelectionModel().select(tab);
                    if (mainController != null) {
                        mainController.onSave(new javafx.event.ActionEvent());
                    } else {
                        vm.save(of);
                    }
                    // Wait, if it's still dirty after Save (e.g. user canceled Save As), abort
                    // closing.
                    if (of.isDirty()) {
                        return;
                    }
                } catch (Exception ex) {
                    DialogUtils.error("Error", "Save failed", ex);
                    return;
                }
            }
        }
        if (of != null) {
            forceClose(tab, of);
        } else {
            tabPane.getTabs().remove(tab);
        }
    }

    public void closeTabByOpenFile(OpenFile of) {
        Tab toRemove = null;
        for (Map.Entry<Tab, OpenFile> entry : tabFileMap.entrySet()) {
            if (entry.getValue() == of) {
                toRemove = entry.getKey();
                break;
            }
        }
        if (toRemove != null) {
            forceClose(toRemove, of);
        }
    }

    private void forceClose(Tab tab, OpenFile of) {
        tabFileMap.remove(tab);
        if (of.getPath() != null)
            pathTabMap.remove(of.getPath());
        vm.closeFile(of);
        tabPane.getTabs().remove(tab);
        if (tabPane.getTabs().isEmpty())
            showWelcome(true);
    }

    public void restoreSessionTab(com.ravi.notesapp.model.SessionTab sessionTab) {
        if (sessionTab.getPath() == null)
            return;
        Path p = Path.of(sessionTab.getPath());
        OpenFile of = vm.restoreSessionFile(p, sessionTab.getContent(), sessionTab.isDirty());

        Tab tab = createTab(of);
        int insertPos = tabPane.getTabs().size() > 0 ? tabPane.getTabs().size() - 1 : 0;
        tabPane.getTabs().add(insertPos, tab);
        pathTabMap.put(p, tab);
        tabFileMap.put(tab, of);
        showWelcome(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Close Saved Files
    // ─────────────────────────────────────────────────────────────────────

    public void closeSavedFiles() {
        java.util.List<Tab> tabsToClose = new java.util.ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            OpenFile of = tabFileMap.get(tab);
            if (of != null && !of.isDirty()) {
                tabsToClose.add(tab);
            }
        }
        for (Tab tab : tabsToClose) {
            OpenFile of = tabFileMap.get(tab);
            forceClose(tab, of);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save helpers (called from MainController)
    // ─────────────────────────────────────────────────────────────────────

    public void saveActive() throws IOException {
        vm.saveActive();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Notifications from ExplorerController
    // ─────────────────────────────────────────────────────────────────────

    /** Called when a file was renamed so we update the tab. */
    public void notifyRenamed(Path oldPath, Path newPath) {
        vm.updatePath(oldPath, newPath);
        Tab tab = pathTabMap.remove(oldPath);
        if (tab != null)
            pathTabMap.put(newPath, tab);
    }

    /** Called when a file/folder was deleted — close any open tabs. */
    public void notifyDeleted(Path path) {
        List<Path> toClose = new ArrayList<>();
        for (Path p : pathTabMap.keySet()) {
            if (p != null && p.startsWith(path))
                toClose.add(p);
        }
        for (Path p : toClose) {
            Tab tab = pathTabMap.get(p);
            OpenFile of = tabFileMap.get(tab);
            if (of != null)
                forceClose(tab, of);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // In-file search
    // ─────────────────────────────────────────────────────────────────────

    public void showFind() {
        findBar.setVisible(true);
        findBar.setManaged(true);
        Platform.runLater(() -> findField.requestFocus());
    }

    @FXML
    void onCloseFind(ActionEvent e) {
        findBar.setVisible(false);
        findBar.setManaged(false);
        matches.clear();
        lblMatchCount.setText("");
    }

    @FXML
    void onFindKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            onCloseFind(null);
            return;
        }
        if (e.getCode() == KeyCode.ENTER) {
            onFindNext(null);
            return;
        }
        runSearch();
    }

    @FXML
    void onFindCaseSensitiveChanged(ActionEvent e) {
        runSearch();
    }

    @FXML
    void onFindNext(ActionEvent e) {
        if (matches.isEmpty())
            return;
        matchIndex = (matchIndex + 1) % matches.size();
        highlightMatch(matchIndex);
    }

    @FXML
    void onFindPrev(ActionEvent e) {
        if (matches.isEmpty())
            return;
        matchIndex = (matchIndex - 1 + matches.size()) % matches.size();
        highlightMatch(matchIndex);
    }

    private void runSearch() {
        matches.clear();
        matchIndex = 0;
        String query = findField.getText();
        CodeArea area = getActiveCodeArea();

        if (query.isBlank()) {
            lblMatchCount.setText("");
            if (area != null) {
                area.deselect();
            }
            return;
        }

        if (area == null)
            return;

        String text = area.getText();
        String search = chkCaseSensitive.isSelected() ? query : query.toLowerCase();
        String target = chkCaseSensitive.isSelected() ? text : text.toLowerCase();

        int idx = 0;
        while ((idx = target.indexOf(search, idx)) != -1) {
            matches.add(new int[] { idx, idx + search.length() });
            idx += search.length();
        }

        lblMatchCount.setText(matches.isEmpty() ? "No results" : "1/" + matches.size());
        if (!matches.isEmpty()) {
            highlightMatch(0);
        } else {
            area.deselect();
        }
    }

    private void highlightMatch(int index) {
        CodeArea area = getActiveCodeArea();
        if (area == null || matches.isEmpty())
            return;
        int[] m = matches.get(index);
        area.selectRange(m[0], m[1]);
        lblMatchCount.setText((index + 1) + "/" + matches.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Find in Files (global) — delegate to a dialog
    // ─────────────────────────────────────────────────────────────────────

    public void showFindInFiles() {
        ExplorerViewModel explorerVM = mainController.explorerPaneController.vm;
        Path root = explorerVM != null ? explorerVM.getRootFolder() : null;
        if (root == null) {
            DialogUtils.info("Find in Files", "Open a folder first.");
            return;
        }

        DialogUtils.askText("Find in Files", "Search across all files in " + root.getFileName(),
                "Search query:", "")
                .filter(q -> !q.isBlank())
                .ifPresent(query -> {
                    try {
                        List<SearchService.SearchResult> results = searchService.searchInDirectory(root, query, false,
                                200);
                        showFindInFilesResults(results, root, query);
                    } catch (IOException ex) {
                        DialogUtils.error("Error", "Search failed", ex);
                    }
                });
    }

    private void showFindInFilesResults(List<SearchService.SearchResult> results, Path root, String query) {
        if (results.isEmpty()) {
            DialogUtils.info("Find in Files", "No matches found for: " + query);
            return;
        }
        // Show in a dialog with a ListView
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Find in Files — \"" + query + "\"  (" + results.size() + " matches)");
        dialog.setHeaderText(null);

        ListView<String> list = new ListView<>();
        list.setPrefSize(700, 400);
        for (SearchService.SearchResult r : results) {
            list.getItems().add(String.format("[%s:%d]  %s",
                    r.getDisplayPath(root), r.lineNumber(), r.lineContent()));
        }
        // Double-click opens the file
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2)
                return;
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx < 0)
                return;
            SearchService.SearchResult r = results.get(idx);
            try {
                openFile(r.file());
            } catch (IOException ex) {
                DialogUtils.error("Error", "Unsupported file type", ex);
            }
            dialog.close();
        });

        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Welcome from editor.fxml button
    // ─────────────────────────────────────────────────────────────────────

    @FXML
    void onOpenFolderFromWelcome(ActionEvent e) {
        mainController.onOpenFolder(e);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Run & Console
    // ─────────────────────────────────────────────────────────────────────

    @FXML
    void onConsoleHeaderPressed(javafx.scene.input.MouseEvent e) {
        if (!editorSplitPane.getDividers().isEmpty()) {
            consoleDragInitialDivider = editorSplitPane.getDividerPositions()[0];
            consoleDragStartY = e.getScreenY();
        }
    }

    @FXML
    void onConsoleHeaderDragged(javafx.scene.input.MouseEvent e) {
        if (!editorSplitPane.getDividers().isEmpty()) {
            double deltaY = e.getScreenY() - consoleDragStartY;
            double totalHeight = editorSplitPane.getHeight();
            if (totalHeight > 0) {
                double deltaPosition = deltaY / totalHeight;
                double newPosition = consoleDragInitialDivider + deltaPosition;
                newPosition = Math.max(0.0, Math.min(1.0, newPosition));
                editorSplitPane.setDividerPositions(newPosition);
            }
        }
    }

    @FXML
    void onClearConsole(ActionEvent e) {
        consoleOutput.clear();
    }

    @FXML
    void onCloseConsole(ActionEvent e) {
        editorSplitPane.getItems().remove(consolePane);
    }

    public void runActiveFile() {
        OpenFile active = vm.getActiveFile();
        if (active == null || active.getPath() == null) {
            DialogUtils.info("Run", "No active file to run, or file is not saved to disk.");
            return;
        }

        // Auto-save before running
        try {
            if (active.isDirty()) {
                vm.saveActive();
            }
        } catch (IOException ex) {
            DialogUtils.error("Save Error", "Could not save file before running", ex);
            return;
        }

        if (!editorSplitPane.getItems().contains(consolePane)) {
            editorSplitPane.getItems().add(consolePane);
            editorSplitPane.setDividerPositions(0.7);
        }
        consoleOutput.clear();
        consoleOutput.appendText("> Running: " + active.getFileName() + "\n");
        if (consoleInput != null) {
            consoleInput.clear();
            consoleInput.setDisable(false);
            consoleInput.requestFocus();
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Process process = executionService.startProcess(active.getPath());
                activeProcess = process;

                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(process.getInputStream())) {
                    char[] buffer = new char[1024];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, read);
                        Platform.runLater(() -> {
                            consoleOutput.appendText(chunk);
                        });
                    }
                }

                int exitCode = process.waitFor();
                Platform.runLater(() -> {
                    consoleOutput.appendText("\n> Process exited with code " + exitCode + "\n");
                    if (consoleInput != null) {
                        consoleInput.setDisable(true);
                    }
                    activeProcess = null;
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            consoleOutput.appendText("\n> Error: " + ex.getMessage() + "\n");
        });

        new Thread(task).start();
    }

    @FXML
    void onConsoleInputEntered(ActionEvent e) {
        if (activeProcess != null && activeProcess.isAlive()) {
            try {
                String input = consoleInput.getText();
                java.io.OutputStream out = activeProcess.getOutputStream();
                out.write((input + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
                consoleOutput.appendText("❯ " + input + "\n");
                consoleInput.clear();
            } catch (IOException ex) {
                consoleOutput.appendText("\n> Error writing input: " + ex.getMessage() + "\n");
            }
        } else {
            consoleInput.clear();
            consoleInput.setDisable(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    public CodeArea getActiveCodeArea() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        if (t == null)
            return null;
        if (t.getContent() instanceof VirtualizedScrollPane) {
            return (CodeArea) ((VirtualizedScrollPane<?>) t.getContent()).getContent();
        }
        return t.getContent() instanceof CodeArea ? (CodeArea) t.getContent() : null;
    }

    public String getActiveFileContent() {
        CodeArea area = getActiveCodeArea();
        if (area != null) {
            return area.getText();
        }
        return "";
    }

    private void showWelcome(boolean show) {
        welcomePane.setVisible(show);
        tabPane.setVisible(!show);
    }
}
