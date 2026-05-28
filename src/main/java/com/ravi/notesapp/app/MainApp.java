package com.ravi.notesapp.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.Objects;

public class MainApp extends Application {

    public static final String APP_NAME = "Notepad_Pro";
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Load custom fonts
        try (var fontStream = getClass().getResourceAsStream("/com/ravi/notesapp/fonts/ShareTech-Regular.ttf")) {
            if (fontStream != null) {
                Font.loadFont(fontStream, 12);
            }
        } catch (Exception e) {
            System.err.println("Failed to load ShareTech font: " + e.getMessage());
        }

        try (var fontStream = getClass().getResourceAsStream("/com/ravi/notesapp/fonts/JetBrainsMono-Regular.ttf")) {
            if (fontStream != null) {
                Font.loadFont(fontStream, 12);
            }
        } catch (Exception e) {
            System.err.println("Failed to load JetBrainsMono font: " + e.getMessage());
        }

        URL fxmlUrl = getClass().getResource("/com/ravi/notesapp/view/main.fxml");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(800, Math.min(1280, screenBounds.getWidth() * 0.85));
        double height = Math.max(500, Math.min(800, screenBounds.getHeight() * 0.85));
        Scene scene = new Scene(root, width, height);

        // Apply default dark theme
        URL cssUrl = getClass().getResource("/com/ravi/notesapp/styles/dark.css");
        scene.getStylesheets().add(Objects.requireNonNull(cssUrl).toExternalForm());

        URL iconUrl = getClass().getResource("/com/ravi/notesapp/app_icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
        }

        stage.setTitle(APP_NAME);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        // Pass command line arguments (e.g. from Open With) to the controller
        com.ravi.notesapp.controller.MainController controller = loader.getController();
        var parameters = getParameters();
        if (parameters != null && !parameters.getRaw().isEmpty()) {
            String filePath = parameters.getRaw().get(0);
            controller.openFileFromArgs(filePath);
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
