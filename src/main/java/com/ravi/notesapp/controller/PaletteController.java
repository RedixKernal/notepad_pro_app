package com.ravi.notesapp.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PaletteController {

    @FXML private TextField searchField;
    @FXML private ListView<Path> resultList;

    private Popup popup;
    private MainController mainController;
    private Path currentRoot;
    private List<Path> allFiles = new ArrayList<>();

    public void init(Popup popup, MainController mainController) {
        this.popup = popup;
        this.mainController = mainController;

        resultList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currentRoot.relativize(item).toString());
                }
            }
        });
        
        searchField.textProperty().addListener((obs, oldText, newText) -> filterList(newText));
    }

    public void show(Path rootFolder) {
        this.currentRoot = rootFolder;
        allFiles.clear();
        
        if (rootFolder != null) {
            try (Stream<Path> stream = Files.walk(rootFolder)) {
                stream.filter(Files::isRegularFile)
                      .forEach(allFiles::add);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        searchField.setText("");
        filterList("");
        Platform.runLater(() -> searchField.requestFocus());
    }

    private void filterList(String query) {
        if (query == null || query.isBlank()) {
            resultList.setItems(FXCollections.observableArrayList(allFiles));
        } else {
            String lowerQuery = query.toLowerCase();
            List<Path> filtered = allFiles.stream()
                .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerQuery) || 
                             currentRoot.relativize(p).toString().toLowerCase().contains(lowerQuery))
                .toList();
            resultList.setItems(FXCollections.observableArrayList(filtered));
        }
        if (!resultList.getItems().isEmpty()) {
            resultList.getSelectionModel().selectFirst();
        }
    }

    @FXML
    void onSearchKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) {
            popup.hide();
        } else if (e.getCode() == KeyCode.ENTER) {
            openSelected();
        } else if (e.getCode() == KeyCode.DOWN) {
            resultList.requestFocus();
            resultList.getSelectionModel().selectNext();
        }
    }

    @FXML
    void onListClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            openSelected();
        }
    }

    private void openSelected() {
        Path selected = resultList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            popup.hide();
            try {
                mainController.editorPaneController.openFile(selected);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
