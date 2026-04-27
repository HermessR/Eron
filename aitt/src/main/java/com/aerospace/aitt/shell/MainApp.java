package com.aerospace.aitt.shell;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospace.aitt.core.ui.StageManager;
import com.aerospace.aitt.native_.HardwareBridge;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point.
 * Initializes the UI and shows the login screen.
 */
public class MainApp extends Application {
    
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        log.info("Starting JavaFX application");
        
        try {
            // Pre-warm native library in background (improves Hardware module load time)
            CompletableFuture.runAsync(() -> {
                try {
                    if (HardwareBridge.isNativeAvailable()) {
                        log.info("Native library pre-warmed successfully");
                    }
                } catch (Exception e) {
                    log.warn("Native library pre-warm skipped: {}", e.getMessage());
                }
            });
            
            // Initialize stage manager
            StageManager stageManager = StageManager.getInstance();
            stageManager.init(primaryStage);
            
            // Show login screen
            stageManager.switchScene("/fxml/login.fxml");
            
            log.info("Application UI initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize application UI", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    @Override
    public void stop() {
        log.info("Application shutting down");
        // Cleanup resources if needed
    }
}
