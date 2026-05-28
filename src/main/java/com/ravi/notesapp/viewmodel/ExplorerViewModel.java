package com.ravi.notesapp.viewmodel;

import com.ravi.notesapp.model.FileNode;
import com.ravi.notesapp.model.RecentFolder;
import com.ravi.notesapp.service.FileService;
import com.ravi.notesapp.service.RecentService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * ViewModel for the left-panel file explorer.
 */
public class ExplorerViewModel {

    private final FileService   fileService;
    private final RecentService recentService;

    private final ObjectProperty<Path>          rootFolder    = new SimpleObjectProperty<>();
    private final ObjectProperty<TreeItem<Path>> treeRoot     = new SimpleObjectProperty<>();
    private final ObservableList<RecentFolder>  recentFolders = FXCollections.observableArrayList();
    private final StringProperty                filterText    = new SimpleStringProperty("");

    public ExplorerViewModel(FileService fileService, RecentService recentService) {
        this.fileService   = fileService;
        this.recentService = recentService;
        refreshRecent();
    }

    // ---------- Open folder ----------
    public void openFolder(Path folder) throws IOException {
        rootFolder.set(folder);
        recentService.addRecent(folder);
        refreshRecent();
        buildTree(folder);
    }

    // ---------- Refresh ----------
    public void refresh() throws IOException {
        if (rootFolder.get() != null) {
            buildTree(rootFolder.get());
        }
    }

    // ---------- Tree building ----------
    private void buildTree(Path folder) throws IOException {
        TreeItem<Path> root = createItem(folder);
        root.setExpanded(true);
        populateItem(root);
        treeRoot.set(root);
    }

    public void populateItem(TreeItem<Path> parent) {
        parent.getChildren().clear();
        Path path = parent.getValue();
        if (!Files.isDirectory(path)) return;
        try {
            List<Path> children = fileService.listChildren(path);
            for (Path child : children) {
                TreeItem<Path> item = createItem(child);
                parent.getChildren().add(item);
                // Add lazy-load placeholder for directories
                if (Files.isDirectory(child)) {
                    item.getChildren().add(new TreeItem<>()); // dummy
                    item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                        if (isExpanded && item.getChildren().size() == 1
                                && item.getChildren().get(0).getValue() == null) {
                            populateItem(item);
                        }
                    });
                }
            }
        } catch (IOException ignored) {}
    }

    private TreeItem<Path> createItem(Path path) {
        return new TreeItem<>(path);
    }

    // ---------- CRUD on tree ----------
    public void createFile(Path parent, String name) throws IOException {
        Path newFile = parent.resolve(name);
        fileService.createFile(newFile);
    }

    public void createFolder(Path parent, String name) throws IOException {
        Path newFolder = parent.resolve(name);
        fileService.createDirectory(newFolder);
    }

    public Path rename(Path oldPath, String newName) throws IOException {
        return fileService.rename(oldPath, newName);
    }

    public boolean delete(Path path) throws IOException {
        return fileService.delete(path);
    }

    // ---------- Recent folders ----------
    private void refreshRecent() {
        recentFolders.setAll(recentService.getRecentFolders());
    }

    // ---------- Properties ----------
    public ObjectProperty<Path>          rootFolderProperty()  { return rootFolder; }
    public Path                          getRootFolder()        { return rootFolder.get(); }
    public ObjectProperty<TreeItem<Path>> treeRootProperty()   { return treeRoot; }
    public TreeItem<Path>                getTreeRoot()          { return treeRoot.get(); }
    public ObservableList<RecentFolder>  getRecentFolders()     { return recentFolders; }
    public StringProperty                filterTextProperty()   { return filterText; }
}
