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

import java.net.URL;
import java.util.ResourceBundle;

public class AIChatController implements Initializable {

    @FXML
    private ScrollPane chatScroll;
    @FXML
    private javafx.scene.layout.VBox chatBox;
    @FXML
    private TextArea promptArea;

    private MainController mainController;
    private final AiService aiService = new AiService();

    // Live stream references
    private Label currentAiLabel;
    private Label currentReasoningLabel;
    private javafx.scene.layout.HBox currentLoaderBox;
    private javafx.scene.control.ScrollPane currentReasoningScroll;
    private String fullCurrentText = "";

    private boolean isAutoScrolling = true;
    private boolean inThinkBlock = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (isAutoScrolling) {
                chatScroll.setVvalue(1.0);
            }
        });

        chatScroll.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (chatScroll.getVmax() > 0 && newVal.doubleValue() < chatScroll.getVmax() - 0.05) {
                isAutoScrolling = false;
            } else if (newVal.doubleValue() >= chatScroll.getVmax() - 0.01) {
                isAutoScrolling = true;
            }
        });
    }

    public void init(MainController mainController) {
        this.mainController = mainController;
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

    private void appendToReasoning(String text) {
        if (currentReasoningLabel != null) {
            String current = currentReasoningLabel.getText();
            if (current.equals("Redix is Thinking...")) {
                currentReasoningLabel.setText(current + "\n" + text.trim());
            } else {
                currentReasoningLabel.setText(current + text);
            }
            currentReasoningScroll.setVvalue(currentReasoningScroll.getVmax());
        }
    }

    private void appendToContent(String text) {
        if (currentAiLabel != null) {
            fullCurrentText += text;
            currentAiLabel.setText(fullCurrentText);
        }
    }

    private void removeLoaderBox() {
        if (currentLoaderBox != null) {
            javafx.scene.layout.VBox bubble = (javafx.scene.layout.VBox) currentLoaderBox.getParent();
            if (bubble != null) {
                bubble.getChildren().remove(currentLoaderBox);
            }
            currentLoaderBox = null;
        }
    }

    @FXML
    void onSend(ActionEvent e) {
        String query = promptArea.getText().trim();
        if (query.isEmpty())
            return;

        promptArea.clear();
        appendMessage("You:\n" + query);

        String context = mainController.editorPaneController.getActiveFileContent();
        String apiUrl = mainController.getVm().getSettings().getAiProviderUrl();
        String model = mainController.getVm().getSettings().getAiModel();
        String apiKey = mainController.getVm().getSettings().getAiApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            appendMessage("AI:\nPlease configure your API Key in the settings (⚙ icon).");
            return;
        }

        isAutoScrolling = true;
        inThinkBlock = false;
        appendMessage("AI:\n[LOADING]");

        aiService.askAiStream(apiUrl, model, apiKey, context, query, token -> {
            if (token == null || token.isEmpty())
                return;
            Platform.runLater(() -> {
                if (token.startsWith("[REASONING]")) {
                    appendToReasoning(token.substring("[REASONING]".length()));
                } else {
                    String t = token;
                    if (t.contains("<think>")) {
                        inThinkBlock = true;
                        t = t.replace("<think>", "");
                    }
                    if (t.contains("</think>")) {
                        inThinkBlock = false;
                        String[] parts = t.split("</think>");
                        if (parts.length > 0) {
                            appendToReasoning(parts[0]);
                        }

                        // Thinking finished, remove the box!
                        // removeLoaderBox();

                        if (parts.length > 1) {
                            appendToContent(parts[1]);
                        }
                    } else if (inThinkBlock) {
                        appendToReasoning(t);
                    } else {
                        // First content outside of thinking! Remove the box!
                        if (!t.trim().isEmpty()) {
                            removeLoaderBox();
                            appendToContent(t);
                        }
                    }
                }
            });
        }).thenRun(() -> {
            Platform.runLater(this::removeLoaderBox);
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                removeLoaderBox();
                if (currentAiLabel != null) {
                    currentAiLabel.setText(currentAiLabel.getText() + "\nError: " + throwable.getMessage());
                }
            });
            return null;
        });
    }

    @FXML
    void onPromptKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
            e.consume();
            onSend(null);
        }
    }

    private void appendMessage(String message) {
        boolean isUser = message.startsWith("You:\n");
        String textContent = isUser ? message.substring(5) : message.substring(4);

        javafx.scene.layout.VBox bubble = new javafx.scene.layout.VBox(8);
        bubble.setMinWidth(0);
        javafx.scene.layout.HBox.setHgrow(bubble, javafx.scene.layout.Priority.ALWAYS);
        bubble.setStyle(
                "-fx-padding: 16 12 16 12; -fx-border-color: transparent transparent -border transparent; -fx-border-width: 1;");

        Label header = new Label(isUser ? "👤 You" : "🤖 Redix");
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-font-size: 14px;");

        javafx.scene.layout.HBox headerBox = new javafx.scene.layout.HBox(header);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(headerBox, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(8);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        toolbar.setMinWidth(0);

        Button copyBtn = new Button("📋");
        copyBtn.getStyleClass().add("icon-btn-small");

        if (!isUser) {
            Button likeBtn = new Button("👍");
            likeBtn.getStyleClass().add("icon-btn-small");
            Button dislikeBtn = new Button("👎");
            dislikeBtn.getStyleClass().add("icon-btn-small");
            toolbar.getChildren().addAll(copyBtn, likeBtn, dislikeBtn);
        } else {
            toolbar.getChildren().add(copyBtn);
        }

        javafx.scene.layout.HBox topRow = new javafx.scene.layout.HBox(headerBox, toolbar);

        if (!isUser && message.equals("AI:\n[LOADING]")) {
            currentLoaderBox = new javafx.scene.layout.HBox(10);
            currentLoaderBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            currentLoaderBox.setStyle(
                    "-fx-background-color: rgba(201, 201, 201, 0.1); -fx-background-radius: 8; -fx-padding: 6 12 6 12;");

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(14, 14);

            currentReasoningLabel = new Label("Redix is Thinking...");
            currentReasoningLabel.setStyle("-fx-text-fill: -text-muted; -fx-font-style: italic; -fx-font-size: 12px;");
            currentReasoningLabel.setWrapText(true);
            currentReasoningLabel.setMinWidth(0);

            currentReasoningScroll = new javafx.scene.control.ScrollPane(currentReasoningLabel);
            currentReasoningScroll.setFitToWidth(true);
            currentReasoningScroll.setMaxHeight(30);
            currentReasoningScroll.setStyle(
                    "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            currentReasoningScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            currentReasoningScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

            currentLoaderBox.getChildren().addAll(spinner, currentReasoningScroll);
            javafx.scene.layout.HBox.setHgrow(currentReasoningScroll, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(currentLoaderBox, javafx.scene.layout.Priority.ALWAYS);

            currentAiLabel = new Label();
            currentAiLabel.setWrapText(true);
            currentAiLabel.setMinWidth(0);
            currentAiLabel.getStyleClass().add("chat-bubble-text");
            fullCurrentText = "";

            copyBtn.setOnAction(evt -> {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(fullCurrentText);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                copyBtn.setText("✔");
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ex) {
                    }
                    Platform.runLater(() -> copyBtn.setText("📋"));
                }).start();
            });

            bubble.getChildren().addAll(topRow, currentLoaderBox, currentAiLabel);
        } else {
            Label textLabel = new Label(textContent);
            textLabel.setWrapText(true);
            textLabel.setMinWidth(0);
            textLabel.getStyleClass().add("chat-bubble-text");

            copyBtn.setOnAction(evt -> {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(textContent);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                copyBtn.setText("✔");
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ex) {
                    }
                    Platform.runLater(() -> copyBtn.setText("📋"));
                }).start();
            });

            bubble.getChildren().addAll(topRow, textLabel);
        }

        javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(bubble);
        container.setMinWidth(0);
        container.prefWidthProperty().bind(chatScroll.widthProperty().subtract(20));
        container.maxWidthProperty().bind(chatScroll.widthProperty().subtract(20));
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            chatBox.getChildren().add(container);
            chatScroll.setVvalue(chatScroll.getVmax());
        });
    }
}
