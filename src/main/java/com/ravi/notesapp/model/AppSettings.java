package com.ravi.notesapp.model;

/**
 * Application-wide settings that are persisted across sessions.
 */
public class AppSettings {
    private String theme = "dark"; // "dark" | "light"
    private double windowWidth = 1280;
    private double windowHeight = 800;
    private double dividerPosition = 0.22;
    private String lastOpenedFolder = null;
    private boolean autoSave = false;
    private int tabSpace = 2;
    public static final String DEFAULT_AI_URL = "https://openrouter.ai/api/v1/";
    public static final String DEFAULT_AI_MODEL = "openrouter/free";
    private static final String DEFAULT_AI_KEY_B64 = "c2stb3ItdjEtNjMyNGRkZmJhM2I2N2EyNTRhOGJkZGM2NmRjNThlZTdmZWE4NjY3ZDMwODcyODJhMmJjNGEwY2RmZDdjZjQ4Mw==";

    private String aiProviderUrl = "";
    private String aiModel = "";
    private String aiApiKey = "";

    public AppSettings() {
    }

    public static String getDefaultApiKey() {
        return new String(java.util.Base64.getDecoder().decode(DEFAULT_AI_KEY_B64));
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(double windowWidth) {
        this.windowWidth = windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    public double getDividerPosition() {
        return dividerPosition;
    }

    public void setDividerPosition(double dividerPosition) {
        this.dividerPosition = dividerPosition;
    }

    public String getLastOpenedFolder() {
        return lastOpenedFolder;
    }

    public void setLastOpenedFolder(String lastOpenedFolder) {
        this.lastOpenedFolder = lastOpenedFolder;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public int getTabSpace() {
        return tabSpace;
    }

    public void setTabSpace(int tabSpace) {
        this.tabSpace = tabSpace;
    }

    public String getAiProviderUrl() {
        return aiProviderUrl;
    }

    public void setAiProviderUrl(String aiProviderUrl) {
        this.aiProviderUrl = aiProviderUrl;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }
}
