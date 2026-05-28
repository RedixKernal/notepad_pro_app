package com.ravi.notesapp.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Central registry of keyboard shortcut definitions.
 */
public final class KeyboardShortcuts {

    private KeyboardShortcuts() {}

    // File operations
    public static final KeyCombination OPEN_FOLDER   = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination NEW_FILE       = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination NEW_FOLDER     = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination SAVE           = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination SAVE_AS        = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination SAVE_ALL       = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
    public static final KeyCombination CLOSE_TAB      = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);

    // Search
    public static final KeyCombination FIND           = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination FIND_IN_FILES  = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);

    // View
    public static final KeyCombination TOGGLE_THEME   = new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination REFRESH_TREE   = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);

    // Delete
    public static final KeyCombination DELETE_FILE    = new KeyCodeCombination(KeyCode.DELETE);
}
