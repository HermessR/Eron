package com.aerospace.aitt.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Persistence store for application settings.
 * Settings are stored as JSON in the user's config directory.
 */
public class SettingsStore {
    
    private static final Logger log = LoggerFactory.getLogger(SettingsStore.class);
    private static SettingsStore instance;
    
    private static final String CONFIG_DIR = ".aitt";
    private static final String SETTINGS_FILE = "settings.json";
    
    private final ObjectMapper objectMapper;
    private final Path settingsPath;
    private AppSettings currentSettings;
    
    private SettingsStore() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Settings stored in user home directory
        Path configDir = Paths.get(System.getProperty("user.home"), CONFIG_DIR);
        settingsPath = configDir.resolve(SETTINGS_FILE);
        
        // Ensure config directory exists
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                log.info("Created config directory: {}", configDir);
            }
        } catch (IOException e) {
            log.error("Failed to create config directory", e);
        }
        
        // Load settings
        currentSettings = load();
    }
    
    public static synchronized SettingsStore getInstance() {
        if (instance == null) {
            instance = new SettingsStore();
        }
        return instance;
    }
    
    /**
     * Returns the current settings.
     */
    public AppSettings getSettings() {
        return currentSettings;
    }
    
    /**
     * Saves the given settings.
     */
    public void save(AppSettings settings) {
        try {
            objectMapper.writeValue(settingsPath.toFile(), settings);
            currentSettings = settings;
            log.info("Settings saved to {}", settingsPath);
        } catch (IOException e) {
            log.error("Failed to save settings", e);
            throw new RuntimeException("Failed to save settings", e);
        }
    }
    
    /**
     * Loads settings from file, or returns defaults if not found.
     */
    private AppSettings load() {
        if (Files.exists(settingsPath)) {
            try {
                AppSettings settings = objectMapper.readValue(settingsPath.toFile(), AppSettings.class);
                log.info("Settings loaded from {}", settingsPath);
                return settings;
            } catch (IOException e) {
                log.warn("Failed to load settings, using defaults", e);
            }
        }
        return new AppSettings();
    }
    
    /**
     * Resets settings to defaults.
     */
    public void resetToDefaults() {
        currentSettings = new AppSettings();
        save(currentSettings);
        log.info("Settings reset to defaults");
    }
    
    /**
     * Returns the path to the settings file.
     */
    public Path getSettingsFilePath() {
        return settingsPath;
    }
}
