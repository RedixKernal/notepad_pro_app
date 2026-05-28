package com.ravi.notesapp.controller;

import com.ravi.notesapp.model.FileNode;
import com.ravi.notesapp.model.RecentFolder;
import com.ravi.notesapp.util.DialogUtils;
import com.ravi.notesapp.util.FileUtils;
import com.ravi.notesapp.viewmodel.ExplorerViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class ExplorerController implements Initializable {

    @FXML
    private TreeView<Path> fileTree;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnClearSearch;
    @FXML
    private ToggleButton btnToggleTree;
    @FXML
    private ListView<String> recentList;
    @FXML
    private VBox recentPanel;

    ExplorerViewModel vm;
    private MainController mainController;

    public ExplorerViewModel getVm() {
        return vm;
    }

    // Tracks selection for CRUD ops
    private TreeItem<Path> selectedItem;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configured later in init()
    }

    public void init(ExplorerViewModel vm, MainController mainController) {
        this.vm = vm;
        this.mainController = mainController;

        // Custom cell factory: icon + name
        fileTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("tree-folder", "tree-file");
                } else {
                    boolean isDir = Files.isDirectory(path);
                    String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
                    
                    if (!isDir && name.contains(".")) {
                        String ext = name.substring(name.lastIndexOf('.') + 1);
                        javafx.scene.image.Image img = com.ravi.notesapp.util.LanguageIconMap.getIconForExtension(ext);
                        if (img != null) {
                            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                            imgView.setFitWidth(16);
                            imgView.setFitHeight(16);
                            setGraphic(imgView);
                            setText(name);
                        } else {
                            String icon = FileUtils.getIcon(path, isDir);
                            setText(icon + " " + name);
                            setGraphic(null);
                        }
                    } else {
                        String icon = FileUtils.getIcon(path, isDir);
                        setText(icon + " " + name);
                        setGraphic(null);
                    }
                    
                    getStyleClass().removeAll("tree-folder", "tree-file");
                    getStyleClass().add(isDir ? "tree-folder" : "tree-file");

                    // Context menu
                    setContextMenu(buildContextMenu(path, isDir));
                }
            }
        });

        // Bind tree root
        vm.treeRootProperty().addListener((obs, o, root) -> {
            fileTree.setRoot(root);
            fileTree.setShowRoot(true);
        });

        // Track selection
        fileTree.getSelectionModel().selectedItemProperty().addListener((obs, o, item) -> {
            selectedItem = item;
        });

        // Recent list
        refreshRecentList();
        vm.getRecentFolders().addListener(
                (javafx.collections.ListChangeListener<RecentFolder>) c -> refreshRecentList());

        // Show/hide recent panel based on whether a folder is open
        vm.rootFolderProperty().addListener((obs, o, root) -> {
            recentPanel.setVisible(root == null);
            recentPanel.setManaged(root == null);
        });
    }

    // ──────────────── Open folder ─────────────────────────────────────────
    public void openFolder(Path folder) throws IOException {
        vm.openFolder(folder);
        mainController.notifyFolderOpened(folder);
    }

    // ──────────────── Tree click ──────────────────────────────────────────
    @FXML
    void onTreeClick(MouseEvent e) {
        TreeItem<Path> item = fileTree.getSelectionModel().getSelectedItem();
        if (item == null || item.getValue() == null)
            return;
        Path path = item.getValue();

        if (e.getClickCount() >= 1 && Files.isRegularFile(path)) {
            // Single-click → open in editor
            if (FileUtils.isBinary(path)) {
                DialogUtils.info("Binary File", "Cannot open binary file: " + path.getFileName());
                return;
            }
            try {
                editorOpen(path);
            } catch (IOException ex) {
                DialogUtils.error("Error", "Unsupported file type", ex);
            }
        }
    }

    @FXML
    void onTreeKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            TreeItem<Path> item = fileTree.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null && Files.isRegularFile(item.getValue())) {
                try {
                    editorOpen(item.getValue());
                } catch (IOException ex) {
                    DialogUtils.error("Error", "Unsupported file type", ex);
                }
            }
        }
        if (e.getCode() == KeyCode.DELETE)
            onDelete(null);
        if (e.getCode() == KeyCode.F2)
            onRename(null);
    }

    private void editorOpen(Path path) throws IOException {
        // Delegate to MainController's EditorController
        mainController.editorPaneController.openFile(path);
    }

    // ──────────────── CRUD handlers ───────────────────────────────────────

    @FXML
    public void onNewFile(ActionEvent e) {
        newFileIn(getParentForNew());
    }

    public void newFileIn(Path parent) {
        if (parent == null) {
            DialogUtils.info("Info", "Open a folder first.");
            return;
        }
        DialogUtils.askText("New File", "Create a new file", "File name:", "untitled.txt")
                .filter(n -> !n.isBlank())
                .ifPresent(name -> {
                    try {
                        vm.createFile(parent, name);
                        vm.refresh();
                    } catch (IOException ex) {
                        DialogUtils.error("Error", "Cannot create file", ex);
                    }
                });
    }

    @FXML
    public void onNewFolder(ActionEvent e) {
        newFolderIn(getParentForNew());
    }

    public void newFolderIn(Path parent) {
        if (parent == null) {
            DialogUtils.info("Info", "Open a folder first.");
            return;
        }
        DialogUtils.askText("New Folder", "Create a new folder", "Folder name:", "new-folder")
                .filter(n -> !n.isBlank())
                .ifPresent(name -> {
                    try {
                        vm.createFolder(parent, name);
                        vm.refresh();
                    } catch (IOException ex) {
                        DialogUtils.error("Error", "Cannot create folder", ex);
                    }
                });
    }

    @FXML
    public void onRename(ActionEvent e) {
        if (selectedItem == null || selectedItem.getValue() == null)
            return;
        renamePath(selectedItem.getValue());
    }

    public void renamePath(Path path) {
        String current = path.getFileName() != null ? path.getFileName().toString() : "";
        DialogUtils.askText("Rename", "Rename \"" + current + "\"", "New name:", current)
                .filter(n -> !n.isBlank() && !n.equals(current))
                .ifPresent(newName -> {
                    try {
                        Path newPath = vm.rename(path, newName);
                        // Notify editor about path change
                        mainController.editorPaneController.notifyRenamed(path, newPath);
                        vm.refresh();
                    } catch (IOException ex) {
                        DialogUtils.error("Error", "Cannot rename", ex);
                    }
                });
    }

    @FXML
    public void onDelete(ActionEvent e) {
        Path target = null;
        if (selectedItem != null && selectedItem.getValue() != null) {
            target = selectedItem.getValue();
        } else {
            com.ravi.notesapp.model.OpenFile active = mainController.getVm().getEditorViewModel().getActiveFile();
            if (active != null && active.getPath() != null) {
                target = active.getPath();
            }
        }
        if (target != null) {
            deletePath(target);
        }
    }

    public void deletePath(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        boolean isDir = java.nio.file.Files.isDirectory(path);
        String typeLabel = isDir ? "folder" : "file";
        if (!DialogUtils.confirm(
                "Move to Recycle Bin",
                "Move \"" + name + "\" to Recycle Bin?",
                "The " + typeLabel + " will be moved to the Recycle Bin and can be restored from there."))
            return;
        try {
            boolean trashed = vm.delete(path);
            mainController.editorPaneController.notifyDeleted(path);
            vm.refresh();
            if (!trashed) {
                // Fallback: was permanently deleted — inform the user
                DialogUtils.info("Deleted",
                        "\"" + name + "\" was permanently deleted\n(Recycle Bin not supported on this system).");
            }
        } catch (IOException ex) {
            DialogUtils.error("Error", "Cannot delete \"" + name + "\"", ex);
        }
    }

    @FXML
    void onCollapseAll(ActionEvent e) {
        collapseAll(fileTree.getRoot());
        if (fileTree.getRoot() != null)
            fileTree.getRoot().setExpanded(true);
    }

    // @FXML
    // void onToggleTree(ActionEvent e) {
    // boolean visible = btnToggleTree.isSelected();
    // fileTree.setVisible(visible);
    // fileTree.setManaged(visible);
    // btnToggleTree.setText(visible ? "📂" : "📁");
    // Tooltip tooltip = btnToggleTree.getTooltip();
    // if (tooltip != null) {
    // tooltip.setText(visible ? "Hide File Tree" : "Show File Tree");
    // }
    // }

    // ──────────────── Search / Filter ─────────────────────────────────────

    @FXML
    void onSearchKeyReleased(KeyEvent e) {
        String filter = searchField.getText().trim().toLowerCase();
        if (filter.isEmpty()) {
            fileTree.setRoot(vm.getTreeRoot());
        } else {
            buildFilteredTree(filter);
        }
    }

    @FXML
    void onClearSearch(ActionEvent e) {
        searchField.clear();
        fileTree.setRoot(vm.getTreeRoot());
    }

    private void buildFilteredTree(String filter) {
        Path rootPath = vm.getRootFolder();
        if (rootPath == null) return;

        TreeItem<Path> filteredRoot = new TreeItem<>(rootPath);
        filteredRoot.setExpanded(true);

        try (java.util.stream.Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> matchingPaths = stream
                .filter(p -> !p.equals(rootPath))
                .filter(p -> {
                    String name = p.getFileName() != null ? p.getFileName().toString().toLowerCase() : "";
                    return name.contains(filter);
                })
                .limit(500)
                .toList();

            for (Path path : matchingPaths) {
                addPathToFilteredTree(filteredRoot, rootPath, path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileTree.setRoot(filteredRoot);
    }

    private void addPathToFilteredTree(TreeItem<Path> rootItem, Path rootPath, Path targetPath) {
        Path relative = rootPath.relativize(targetPath);
        TreeItem<Path> currentItem = rootItem;

        Path tempPath = rootPath;
        for (int i = 0; i < relative.getNameCount(); i++) {
            Path part = relative.getName(i);
            tempPath = tempPath.resolve(part);

            TreeItem<Path> childItem = null;
            for (TreeItem<Path> child : currentItem.getChildren()) {
                if (tempPath.equals(child.getValue())) {
                    childItem = child;
                    break;
                }
            }

            if (childItem == null) {
                childItem = new TreeItem<>(tempPath);
                childItem.setExpanded(true);
                currentItem.getChildren().add(childItem);
            }
            currentItem = childItem;
        }
    }

    // ──────────────── Recent list ─────────────────────────────────────────

    @FXML
    void onRecentClick(MouseEvent e) {
        if (e.getClickCount() != 2)
            return;
        int idx = recentList.getSelectionModel().getSelectedIndex();
        if (idx < 0)
            return;
        RecentFolder rf = vm.getRecentFolders().get(idx);
        try {
            openFolder(rf.toPath());
        } catch (IOException ex) {
            DialogUtils.error("Error", "Cannot open folder", ex);
        }
    }

    private void refreshRecentList() {
        List<RecentFolder> folders = vm.getRecentFolders();
        recentList.getItems().setAll(
                folders.stream().map(rf -> "📁  " + rf.getDisplayName()).toList());
        recentPanel.setVisible(!folders.isEmpty() && vm.getRootFolder() == null);
        recentPanel.setManaged(!folders.isEmpty() && vm.getRootFolder() == null);
    }

    // ──────────────── Context menu ────────────────────────────────────────

    private ContextMenu buildContextMenu(Path path, boolean isDir) {
        ContextMenu cm = new ContextMenu();
        if (isDir) {
            MenuItem newFile = new MenuItem("📄  New File");
            MenuItem newFolder = new MenuItem("📁  New Folder");
            newFile.setOnAction(e -> newFileIn(path));
            newFolder.setOnAction(e -> newFolderIn(path));
            cm.getItems().addAll(newFile, newFolder, new SeparatorMenuItem());
        }
        MenuItem rename = new MenuItem("✏️  Rename");
        MenuItem delete = new MenuItem("🗑  Delete");
        MenuItem copyPath = new MenuItem("📋  Copy Path");
        rename.setOnAction(e -> renamePath(path));
        delete.setOnAction(e -> deletePath(path));
        copyPath.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(path.toAbsolutePath().toString());
            cb.setContent(content);
        });
        cm.getItems().addAll(rename, delete, new SeparatorMenuItem(), copyPath);
        return cm;
    }

    // ──────────────── Utility ─────────────────────────────────────────────

    private Path getParentForNew() {
        if (selectedItem == null && vm.getRootFolder() != null)
            return vm.getRootFolder();
        if (selectedItem == null)
            return null;
        Path p = selectedItem.getValue();
        return Files.isDirectory(p) ? p : (p.getParent() != null ? p.getParent() : vm.getRootFolder());
    }

    private TreeItem<Path> findItemForPath(Path path) {
        return findItemRecursive(fileTree.getRoot(), path);
    }

    private TreeItem<Path> findItemRecursive(TreeItem<Path> node, Path path) {
        if (node == null)
            return null;
        if (path.equals(node.getValue()))
            return node;
        for (TreeItem<Path> child : node.getChildren()) {
            TreeItem<Path> found = findItemRecursive(child, path);
            if (found != null)
                return found;
        }
        return null;
    }

    private void collapseAll(TreeItem<Path> node) {
        if (node == null)
            return;
        for (TreeItem<Path> child : node.getChildren()) {
            collapseAll(child);
            child.setExpanded(false);
        }
    }
}
