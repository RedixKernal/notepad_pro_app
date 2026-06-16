package com.ravi.notesapp.util;

import javafx.scene.Scene;

import java.net.URL;
import java.util.Objects;

/**
 * Handles dark / light theme switching by swapping CSS stylesheets.
 */
public final class ThemeUtils {

    public static final String DARK  = "dark";
    public static final String LIGHT = "light";

    private static String currentTheme = getSystemTheme();

    public static String getSystemTheme() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Process process = Runtime.getRuntime().exec("reg query \"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme");
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("0x1")) {
                        return LIGHT;
                    }
                }
            } catch (Exception e) {
                // Ignore and default to dark
            }
        }
        return DARK;
    }

    private ThemeUtils() {}

    public static void applyTheme(Scene scene, String theme) {
        currentTheme = theme;
        scene.getStylesheets().clear();

        String cssPath = "/com/ravi/notesapp/styles/" + theme + ".css";
        URL url = ThemeUtils.class.getResource(cssPath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    public static void toggleTheme(Scene scene) {
        applyTheme(scene, DARK.equals(currentTheme) ? LIGHT : DARK);
    }

    public static String getCurrentTheme() { return currentTheme; }

    public static boolean isDark() { return DARK.equals(currentTheme); }
}
