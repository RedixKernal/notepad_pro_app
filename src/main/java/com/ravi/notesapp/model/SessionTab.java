package com.ravi.notesapp.model;

public class SessionTab {
    private String path;
    private String content;
    private boolean isDirty;
    
    public SessionTab() {}
    
    public SessionTab(String path, String content, boolean isDirty) {
        this.path = path;
        this.content = content;
        this.isDirty = isDirty;
    }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public boolean isDirty() { return isDirty; }
    public void setDirty(boolean dirty) { isDirty = dirty; }
}
