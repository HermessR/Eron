package com.aerospace.aitt.core.ui;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Base controller providing common functionality for all FXML controllers.
 */
public abstract class BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);
    
    protected Stage stage;
    
    /**
     * Sets the stage reference for this controller.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Returns the current stage.
     */
    protected Stage getStage() {
        return stage;
    }
    
    /**
     * Called after the FXML is loaded. Override for initialization.
     */
    @FXML
    public void initialize() {
        // Override in subclasses
    }
    
    /**
     * Called when the view is shown.
     */
    public void onShow() {
        // Override in subclasses
    }
    
    /**
     * Called when the view is hidden.
     */
    public void onHide() {
        // Override in subclasses
    }
    
    /**
     * Called when the controller is being closed/disposed.
     */
    public void onClose() {
        // Override in subclasses for cleanup
    }
    
    /**
     * Runs code on the JavaFX Application Thread.
     */
    protected void runOnUiThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
    
    /**
     * Runs code asynchronously and updates UI with result.
     */
    protected <T> void runAsync(Supplier<T> backgroundTask, java.util.function.Consumer<T> uiCallback) {
        CompletableFuture.supplyAsync(backgroundTask)
            .thenAcceptAsync(uiCallback, Platform::runLater);
    }
    
    /**
     * Shows an information alert.
     */
    protected void showInfo(String title, String message) {
        runOnUiThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows a warning alert.
     */
    protected void showWarning(String title, String message) {
        runOnUiThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows an error alert.
     */
    protected void showError(String title, String message) {
        runOnUiThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows a confirmation dialog and returns the result.
     */
    protected boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Applies application styling to alerts.
     */
    private void styleAlert(Alert alert) {
        URL cssResource = getClass().getResource("/css/style.css");
        if (cssResource != null) {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        } else {
            log.warn("Could not load CSS stylesheet for alert dialog");
        }
    }
    
    /**
     * Navigates to a named view.
     */
    protected void navigateTo(String viewName) {
        String fxmlPath = "/fxml/" + viewName + ".fxml";
        StageManager.getInstance().switchScene(fxmlPath);
    }
    
    /**
     * Returns the window (stage) for this controller.
     */
    protected javafx.stage.Window getWindow() {
        return stage != null ? stage : StageManager.getInstance().getPrimaryStage();
    }
}
