package com.ravi.notesapp.controller;

import com.ravi.notesapp.model.AppSettings;
import com.ravi.notesapp.service.AiService;
import com.ravi.notesapp.util.DialogUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.scene.Node;

import java.net.URL;
import java.util.ResourceBundle;

public class AIChatController implements Initializable {

    @FXML private WebView chatWebView;
    @FXML private TextArea promptArea;
    @FXML private Button sendBtn;

    private MainController mainController;
    private final AiService aiService = new AiService();
    private Node originalSendGraphic;
    
    private String currentAiId = null;
    private String fullCurrentText = "";
    private Bridge jsBridge;
    private boolean isGenerating = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        originalSendGraphic = sendBtn.getGraphic();
        
        // Setup WebView bridge and theme when loaded
        chatWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                jsBridge = new Bridge(chatWebView);
                netscape.javascript.JSObject win = (netscape.javascript.JSObject) chatWebView.getEngine().executeScript("window");
                win.setMember("app", jsBridge);
                
                boolean isDark = "dark".equals(com.ravi.notesapp.util.ThemeUtils.getSystemTheme());
                chatWebView.getEngine().executeScript("setTheme(" + isDark + ")");
            }
        });
        
        URL htmlUrl = getClass().getResource("/com/ravi/notesapp/view/chat_view.html");
        if(htmlUrl != null) {
            chatWebView.getEngine().load(htmlUrl.toExternalForm());
        }
    }

    public void init(MainController mainController) {
        this.mainController = mainController;
    }

    public void updateTheme(boolean isDark) {
        if (chatWebView != null && chatWebView.getEngine() != null) {
            try {
                chatWebView.getEngine().executeScript("setTheme(" + isDark + ")");
            } catch (Exception ignored) {}
        }
    }

    @FXML
    void onSettings(ActionEvent e) {
        AppSettings settings = mainController.getVm().getSettings();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Settings");
        dialog.setHeaderText("Configure your AI Provider (OpenAI, Gemini, OpenRouter, DeepSeek)");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 10, 10, 10));

        TextField urlField = new TextField(settings.getAiProviderUrl());
        urlField.setPromptText("e.g. https://api.openai.com/v1/");
        urlField.setPrefWidth(300);

        TextField modelField = new TextField(settings.getAiModel());
        modelField.setPromptText("e.g. gpt-4o-mini");

        PasswordField keyField = new PasswordField();
        keyField.setText(settings.getAiApiKey());

        grid.add(new Label("API Base URL:"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("Model Name:"), 0, 1);
        grid.add(modelField, 1, 1);
        grid.add(new Label("API Key:"), 0, 2);
        grid.add(keyField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        DialogUtils.styleDialog(dialog);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                settings.setAiProviderUrl(urlField.getText().trim());
                settings.setAiModel(modelField.getText().trim());
                settings.setAiApiKey(keyField.getText().trim());
                mainController.getVm().getSettingsService().save();
            }
        });
    }

    @FXML
    void onPromptKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            if (e.isControlDown()) {
                promptArea.insertText(promptArea.getCaretPosition(), "\n");
                e.consume();
            } else {
                e.consume();
                onSend(null);
            }
        }
    }

    @FXML
    void onSend(ActionEvent e) {
        if (isGenerating) return;
        
        String query = promptArea.getText().trim();
        if (query.isEmpty()) return;
        
        isGenerating = true;

        AppSettings settings = mainController.getVm().getSettings();
        String apiUrl = settings.getAiProviderUrl();
        String apiKey = settings.getAiApiKey();
        String model = settings.getAiModel();

        if (apiUrl == null || apiUrl.isEmpty()) apiUrl = AppSettings.DEFAULT_AI_URL;
        if (model == null || model.isEmpty()) model = AppSettings.DEFAULT_AI_MODEL;
        if (apiKey == null || apiKey.isEmpty()) apiKey = AppSettings.getDefaultApiKey();

        promptArea.clear();
        String context = "";
        if (mainController != null && mainController.editorPaneController != null) {
            context = mainController.editorPaneController.getActiveFileContent();
        }

        promptArea.setDisable(true);
        
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(16, 16);
        sendBtn.setGraphic(pi);

        String userId = java.util.UUID.randomUUID().toString();
        executeJS("appendMessage('" + userId + "', true, " + escapeJS(query) + ")");
        
        currentAiId = java.util.UUID.randomUUID().toString();
        executeJS("appendMessage('" + currentAiId + "', false, '[LOADING]')");
        fullCurrentText = "";

        aiService.askAiStream(apiUrl, model, apiKey, context, query, token -> {
            if (token == null || token.isEmpty()) return;
            Platform.runLater(() -> {
                fullCurrentText += token;
                // Basic cleanup for reasoning tags (hide them)
                String cleaned = fullCurrentText.replaceAll("(?s)<think>.*?</think>", "");
                if (fullCurrentText.contains("<think>") && !fullCurrentText.contains("</think>")) {
                    cleaned = fullCurrentText.substring(0, fullCurrentText.indexOf("<think>"));
                }
                executeJS("updateMessage('" + currentAiId + "', " + escapeJS(cleaned) + ")");
            });
        }).thenRun(() -> {
            Platform.runLater(this::resetInput);
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                resetInput();
                fullCurrentText += "\n\n**Error:** " + throwable.getMessage();
                executeJS("updateMessage('" + currentAiId + "', " + escapeJS(fullCurrentText) + ")");
            });
            return null;
        });
    }

    private void resetInput() {
        sendBtn.setGraphic(originalSendGraphic);
        isGenerating = false;
        promptArea.setDisable(false);
        promptArea.requestFocus();
    }

    private void executeJS(String script) {
        try {
            chatWebView.getEngine().executeScript(script);
        } catch (Exception ex) {
            System.err.println("JS Error: " + ex.getMessage());
        }
    }

    private String escapeJS(String input) {
        if (input == null) return "null";
        String safe = input.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("'", "\\'")
                           .replace("\n", "\\n")
                           .replace("\r", "");
        return "'" + safe + "'";
    }

    public static class Bridge {
        private final WebView chatWebView;

        public Bridge(WebView chatWebView) {
            this.chatWebView = chatWebView;
        }

        public void copyText(String id) {
            Platform.runLater(() -> {
                try {
                    String text = (String) chatWebView.getEngine().executeScript("getRawText('" + id + "')");
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(text);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                } catch (Exception ex) {
                    System.err.println("Copy Failed: " + ex.getMessage());
                }
            });
        }

        public void reportText(String id) {
            Platform.runLater(() -> {
                try {
                    String text = (String) chatWebView.getEngine().executeScript("getRawText('" + id + "')");
                    boolean confirm = DialogUtils.confirm(
                        "Report Content", 
                        "Report inappropriate AI content", 
                        "Are you sure you want to report this response?"
                    );
                    if (confirm) {
                        try {
                            String auditData = "Report Date: " + java.time.LocalDateTime.now() + "\nMessage:\n" + text + "\n\n";
                            java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("user.home"), ".notespad");
                            if (!java.nio.file.Files.exists(dir)) {
                                java.nio.file.Files.createDirectories(dir);
                            }
                            java.nio.file.Files.writeString(
                                dir.resolve("ai_audit_reports.log"),
                                auditData,
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND
                            );
                            DialogUtils.info("Report Submitted", "Thank you. The content has been reported and logged for auditing.");
                        } catch (java.io.IOException ex) {
                            System.err.println("Failed to write audit log: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Report Failed: " + ex.getMessage());
                }
            });
        }
    }
}
