package com.aerospace.aitt.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Application settings model.
 * Stores user preferences for TRACE32, flashing, output, and appearance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppSettings {
    
    // TRACE32 Settings
    private String trace32ExecutablePath = "";
    private int connectionTimeout = 30; // seconds
    
    // Flashing Settings
    private String defaultTargetProfile = "";
    private boolean autoValidate = true;
    private boolean confirmBeforeFlash = true;
    private boolean confirmBeforeAbort = true;
    
    // Output Settings
    private String defaultOutputDirectory = "";
    private boolean autoGenerateFilename = true;
    private boolean openOutputFolderAfterGenerate = false;
    
    // Appearance Settings
    private String theme = "dark"; // dark, light
    private int logMaxLines = 1000;
    private boolean showTimestampInLog = true;
    private boolean wrapLogText = true;
    
    // Getters and Setters - TRACE32
    public String getTrace32ExecutablePath() {
        return trace32ExecutablePath;
    }
    
    public void setTrace32ExecutablePath(String trace32ExecutablePath) {
        this.trace32ExecutablePath = trace32ExecutablePath;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    // Getters and Setters - Flashing
    public String getDefaultTargetProfile() {
        return defaultTargetProfile;
    }
    
    public void setDefaultTargetProfile(String defaultTargetProfile) {
        this.defaultTargetProfile = defaultTargetProfile;
    }
    
    public boolean isAutoValidate() {
        return autoValidate;
    }
    
    public void setAutoValidate(boolean autoValidate) {
        this.autoValidate = autoValidate;
    }
    
    public boolean isConfirmBeforeFlash() {
        return confirmBeforeFlash;
    }
    
    public void setConfirmBeforeFlash(boolean confirmBeforeFlash) {
        this.confirmBeforeFlash = confirmBeforeFlash;
    }
    
    public boolean isConfirmBeforeAbort() {
        return confirmBeforeAbort;
    }
    
    public void setConfirmBeforeAbort(boolean confirmBeforeAbort) {
        this.confirmBeforeAbort = confirmBeforeAbort;
    }
    
    // Getters and Setters - Output
    public String getDefaultOutputDirectory() {
        return defaultOutputDirectory;
    }
    
    public void setDefaultOutputDirectory(String defaultOutputDirectory) {
        this.defaultOutputDirectory = defaultOutputDirectory;
    }
    
    public boolean isAutoGenerateFilename() {
        return autoGenerateFilename;
    }
    
    public void setAutoGenerateFilename(boolean autoGenerateFilename) {
        this.autoGenerateFilename = autoGenerateFilename;
    }
    
    public boolean isOpenOutputFolderAfterGenerate() {
        return openOutputFolderAfterGenerate;
    }
    
    public void setOpenOutputFolderAfterGenerate(boolean openOutputFolderAfterGenerate) {
        this.openOutputFolderAfterGenerate = openOutputFolderAfterGenerate;
    }
    
    // Getters and Setters - Appearance
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public int getLogMaxLines() {
        return logMaxLines;
    }
    
    public void setLogMaxLines(int logMaxLines) {
        this.logMaxLines = logMaxLines;
    }
    
    public boolean isShowTimestampInLog() {
        return showTimestampInLog;
    }
    
    public void setShowTimestampInLog(boolean showTimestampInLog) {
        this.showTimestampInLog = showTimestampInLog;
    }
    
    public boolean isWrapLogText() {
        return wrapLogText;
    }
    
    public void setWrapLogText(boolean wrapLogText) {
        this.wrapLogText = wrapLogText;
    }
    
    /**
     * Creates a copy of the settings.
     */
    public AppSettings copy() {
        AppSettings copy = new AppSettings();
        copy.trace32ExecutablePath = this.trace32ExecutablePath;
        copy.connectionTimeout = this.connectionTimeout;
        copy.defaultTargetProfile = this.defaultTargetProfile;
        copy.autoValidate = this.autoValidate;
        copy.confirmBeforeFlash = this.confirmBeforeFlash;
        copy.confirmBeforeAbort = this.confirmBeforeAbort;
        copy.defaultOutputDirectory = this.defaultOutputDirectory;
        copy.autoGenerateFilename = this.autoGenerateFilename;
        copy.openOutputFolderAfterGenerate = this.openOutputFolderAfterGenerate;
        copy.theme = this.theme;
        copy.logMaxLines = this.logMaxLines;
        copy.showTimestampInLog = this.showTimestampInLog;
        copy.wrapLogText = this.wrapLogText;
        return copy;
    }
}
