package com.ravi.notesapp.util;

import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Centralised helper for common JavaFX dialogs.
 */
public final class DialogUtils {

    private DialogUtils() {}

    // ------------------------------------------------------------------ //
    //  Directory chooser
    // ------------------------------------------------------------------ //

    public static Optional<Path> chooseFolder(Window owner, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File file = chooser.showDialog(owner);
        return file != null ? Optional.of(file.toPath()) : Optional.empty();
    }

    // ------------------------------------------------------------------ //
    //  File chooser
    // ------------------------------------------------------------------ //

    public static Optional<Path> saveAs(Window owner, String title, String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(defaultName);
        File file = chooser.showSaveDialog(owner);
        return file != null ? Optional.of(file.toPath()) : Optional.empty();
    }

    // ------------------------------------------------------------------ //
    //  Text Input Dialog
    // ------------------------------------------------------------------ //

    public static Optional<String> askText(String title, String header, String prompt, String defaultValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
        
        Label label = new Label(prompt);
        label.getStyleClass().add("dialog-prompt-label");
        
        TextField textField = new TextField(defaultValue);
        textField.getStyleClass().add("text-field");
        
        vbox.getChildren().addAll(label, textField);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().setPrefWidth(450);
        
        styleDialog(dialog);
        
        javafx.application.Platform.runLater(textField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return textField.getText();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ------------------------------------------------------------------ //
    //  Confirmation
    // ------------------------------------------------------------------ //

    public static boolean confirm(String title, String header, String body) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(body);
        styleDialog(alert);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    // ------------------------------------------------------------------ //
    //  Error
    // ------------------------------------------------------------------ //

    public static void error(String title, String message, Throwable cause) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        if (cause != null) {
            alert.setContentText(cause.getMessage());
        }
        styleDialog(alert);
        alert.showAndWait();
    }

    // ------------------------------------------------------------------ //
    //  Info
    // ------------------------------------------------------------------ //

    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleDialog(alert);
        alert.showAndWait();
    }

    // ------------------------------------------------------------------ //
    //  Unsaved-changes prompt: Save / Discard / Cancel
    // ------------------------------------------------------------------ //

    public enum UnsavedChoice { SAVE, DISCARD, CANCEL }

    public static UnsavedChoice unsavedChanges(String fileName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Save changes to \"" + fileName + "\"?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType save    = new ButtonType("Save",    ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        ButtonType cancel  = new ButtonType("Cancel",  ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        styleDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancel) return UnsavedChoice.CANCEL;
        return result.get() == save ? UnsavedChoice.SAVE : UnsavedChoice.DISCARD;
    }

    // ------------------------------------------------------------------ //
    public static void styleDialog(Dialog<?> dialog) {
        dialog.setGraphic(null);
        DialogPane dp = dialog.getDialogPane();
        dp.setGraphic(null);
        String css = ThemeUtils.isDark() ? "/com/ravi/notesapp/styles/dark.css" : "/com/ravi/notesapp/styles/light.css";
        java.net.URL resource = DialogUtils.class.getResource(css);
        if (resource != null) {
            dp.getStylesheets().add(resource.toExternalForm());
        }
        
        // Add app icon to the dialog window
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) dp.getScene().getWindow();
            java.net.URL iconUrl = DialogUtils.class.getResource("/com/ravi/notesapp/app_icon.png");
            if (iconUrl != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            // Ignore if the window cannot be cast to Stage or icon is missing
        }
    }
}
