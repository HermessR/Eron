package com.aerospace.aitt.core.ui;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospace.aitt.core.config.AppSettings;
import com.aerospace.aitt.core.config.SettingsStore;

import javafx.scene.Scene;

/**
 * Manages application theme switching.
 */
public class ThemeManager {
    
    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);
    private static ThemeManager instance;
    
    public static final String DARK_THEME = "Dark";
    public static final String LIGHT_THEME = "Light";
    
    private static final String DARK_CSS = "/css/style.css";
    private static final String LIGHT_CSS = "/css/style-light.css";
    
    private volatile String currentTheme = DARK_THEME;
    
    private ThemeManager() {
        // Load saved theme preference
        AppSettings settings = SettingsStore.getInstance().getSettings();
        if (settings.getTheme() != null) {
            currentTheme = settings.getTheme();
        }
    }
    
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Returns the current theme name.
     */
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Returns the CSS path for the given theme.
     */
    public String getThemeCssPath(String theme) {
        return LIGHT_THEME.equalsIgnoreCase(theme) ? LIGHT_CSS : DARK_CSS;
    }
    
    /**
     * Returns the CSS path for the current theme.
     */
    public String getCurrentThemeCssPath() {
        return getThemeCssPath(currentTheme);
    }
    
    /**
     * Applies a theme to the given scene.
     */
    public void applyTheme(Scene scene, String theme) {
        if (scene == null) return;
        
        String cssPath = getThemeCssPath(theme);
        URL cssResource = getClass().getResource(cssPath);
        
        if (cssResource == null) {
            log.warn("Theme CSS not found: {}, falling back to dark theme", cssPath);
            cssResource = getClass().getResource(DARK_CSS);
            if (cssResource == null) {
                log.error("Default theme CSS not found: {}", DARK_CSS);
                return;
            }
        }
        
        String cssUrl = cssResource.toExternalForm();
        
        // Clear existing stylesheets and apply new one
        scene.getStylesheets().clear();
        scene.getStylesheets().add(cssUrl);
        
        currentTheme = theme;
        log.info("Applied theme: {}", theme);
    }
    
    /**
     * Applies the current (saved) theme to the given scene.
     */
    public void applyCurrentTheme(Scene scene) {
        applyTheme(scene, currentTheme);
    }
    
    /**
     * Applies theme to all application windows.
     */
    public void applyThemeGlobally(String theme) {
        currentTheme = theme;
        
        // Apply to main scene via StageManager
        StageManager stageManager = StageManager.getInstance();
        if (stageManager.getPrimaryStage() != null && 
            stageManager.getPrimaryStage().getScene() != null) {
            applyTheme(stageManager.getPrimaryStage().getScene(), theme);
        }
        
        log.info("Theme applied globally: {}", theme);
    }
}
