package com.ravi.notesapp.model;

import java.util.ArrayList;
import java.util.List;

public class SessionState {
    private List<SessionTab> tabs = new ArrayList<>();
    private String activeTabPath;

    public SessionState() {}

    public List<SessionTab> getTabs() { return tabs; }
    public void setTabs(List<SessionTab> tabs) { this.tabs = tabs; }

    public String getActiveTabPath() { return activeTabPath; }
    public void setActiveTabPath(String activeTabPath) { this.activeTabPath = activeTabPath; }
}
