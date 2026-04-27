package com.aerospace.aitt.config.ui;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.BaseController;

import javafx.fxml.FXML;

/**
 * Placeholder controller for the Configuration Module.
 * This module is under development.
 */
@SuppressWarnings("unused") // FXML handlers invoked via reflection
public class ConfigController extends BaseController {
    
    private final AppLogger log = new AppLogger(ConfigController.class);
    
    @Override
    public void initialize() {
        log.info("ConfigController initialized (placeholder)");
    }
    
    @FXML
    private void handleBack() {
        navigateTo("dashboard");
    }
}
