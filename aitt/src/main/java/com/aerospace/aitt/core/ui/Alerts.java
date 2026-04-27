package com.aerospace.aitt.core.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.StageStyle;

/**
 * Static utility class for showing alert dialogs.
 */
public final class Alerts {
    
    private static final Logger log = LoggerFactory.getLogger(Alerts.class);
    
    private Alerts() {}
    
    /**
     * Shows an information alert.
     */
    public static void info(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.INFORMATION, title, content);
        alert.showAndWait();
    }
    
    /**
     * Shows a warning alert.
     */
    public static void warning(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.WARNING, title, content);
        alert.showAndWait();
    }
    
    /**
     * Shows an error alert.
     */
    public static void error(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.ERROR, title, content);
        alert.showAndWait();
    }
    
    /**
     * Shows an error alert with exception details.
     */
    public static void error(String title, String content, Throwable ex) {
        Alert alert = createAlert(Alert.AlertType.ERROR, title, content);
        
        // Create expandable exception area with try-with-resources
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            ex.printStackTrace(pw);
        }
        
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
    
    /**
     * Shows a confirmation dialog.
     * @return true if user clicked OK
     */
    public static boolean confirm(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Shows a confirmation dialog with custom button text.
     */
    public static boolean confirm(String title, String content, String confirmText, String cancelText) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, content);
        
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(confirmButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }
    
    /**
     * Shows an information alert with default title.
     */
    public static void info(String content) {
        info("Information", content);
    }
    
    /**
     * Shows a warning alert with default title.
     */
    public static void warn(String content) {
        warning("Warning", content);
    }
    
    /**
     * Shows an error alert with default title.
     */
    public static void error(String content) {
        error("Error", content);
    }
    
    /**
     * Shows a confirmation dialog with default title.
     */
    public static boolean confirm(String content) {
        return confirm("Confirm", content);
    }
    
    /**
     * Shows a warning confirmation (for destructive actions).
     */
    public static boolean confirmWarning(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.WARNING, title, content);
        
        ButtonType proceedButton = new ButtonType("Proceed", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(proceedButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == proceedButton;
    }
    
    private static Alert createAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.initStyle(StageStyle.DECORATED);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Apply stylesheet with null safety
        URL cssResource = Alerts.class.getResource("/css/style.css");
        if (cssResource != null) {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        } else {
            log.warn("Could not load CSS stylesheet for alert dialog");
        }
        
        return alert;
    }
}
