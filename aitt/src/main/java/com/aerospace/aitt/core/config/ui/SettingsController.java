package com.aerospace.aitt.core.config.ui;

import java.io.File;
import java.util.List;

import com.aerospace.aitt.core.config.AppSettings;
import com.aerospace.aitt.core.config.SettingsStore;
import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.ThemeManager;
import com.aerospace.aitt.flashing.data.ProfileStore;
import com.aerospace.aitt.flashing.model.TargetProfile;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for the Settings dialog.
 */
@SuppressWarnings("unused") // FXML handlers are invoked via reflection
public class SettingsController {
    
    private final AppLogger log = new AppLogger(SettingsController.class);
    
    // TRACE32 Settings
    @FXML private TextField trace32PathField;
    @FXML private Spinner<Integer> timeoutSpinner;
    
    // Flashing Settings
    @FXML private ComboBox<String> defaultProfileCombo;
    @FXML private CheckBox autoValidateCheck;
    @FXML private CheckBox confirmFlashCheck;
    @FXML private CheckBox confirmAbortCheck;
    
    // Output Settings
    @FXML private TextField outputDirField;
    @FXML private CheckBox autoFilenameCheck;
    @FXML private CheckBox openFolderCheck;
    
    // Appearance Settings
    @FXML private ComboBox<String> themeCombo;
    @FXML private Spinner<Integer> logLinesSpinner;
    @FXML private CheckBox showTimestampCheck;
    @FXML private CheckBox wrapLogCheck;
    
    @FXML private TabPane settingsTabs;
    
    private SettingsStore settingsStore;
    private AppSettings originalSettings;
    private ProfileStore profileStore;
    
    @FXML
    public void initialize() {
        log.info("Initializing SettingsController");
        
        settingsStore = SettingsStore.getInstance();
        originalSettings = settingsStore.getSettings().copy();
        profileStore = new ProfileStore();
        
        setupSpinners();
        setupCombos();
        loadSettings();
    }
    
    private void setupSpinners() {
        // Connection timeout: 1-300 seconds
        SpinnerValueFactory<Integer> timeoutFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 30);
        timeoutSpinner.setValueFactory(timeoutFactory);
        
        // Log lines: 100-10000
        SpinnerValueFactory<Integer> logLinesFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 10000, 1000, 100);
        logLinesSpinner.setValueFactory(logLinesFactory);
    }
    
    private void setupCombos() {
        // Load target profiles with null safety
        List<TargetProfile> profiles = profileStore.findAll();
        defaultProfileCombo.getItems().add(""); // Empty = no default
        if (profiles != null) {
            for (TargetProfile profile : profiles) {
                defaultProfileCombo.getItems().add(profile.id());
            }
        }
        
        // Theme options
        themeCombo.getItems().addAll("Dark", "Light");
    }
    
    private void loadSettings() {
        AppSettings settings = settingsStore.getSettings();
        
        // TRACE32
        trace32PathField.setText(settings.getTrace32ExecutablePath() != null ? 
            settings.getTrace32ExecutablePath() : "");
        timeoutSpinner.getValueFactory().setValue(settings.getConnectionTimeout());
        
        // Flashing
        defaultProfileCombo.setValue(settings.getDefaultTargetProfile() != null ? 
            settings.getDefaultTargetProfile() : "");
        autoValidateCheck.setSelected(settings.isAutoValidate());
        confirmFlashCheck.setSelected(settings.isConfirmBeforeFlash());
        confirmAbortCheck.setSelected(settings.isConfirmBeforeAbort());
        
        // Output
        outputDirField.setText(settings.getDefaultOutputDirectory() != null ? 
            settings.getDefaultOutputDirectory() : "");
        autoFilenameCheck.setSelected(settings.isAutoGenerateFilename());
        openFolderCheck.setSelected(settings.isOpenOutputFolderAfterGenerate());
        
        // Appearance
        themeCombo.setValue(settings.getTheme() != null ? settings.getTheme() : "Dark");
        logLinesSpinner.getValueFactory().setValue(settings.getLogMaxLines());
        showTimestampCheck.setSelected(settings.isShowTimestampInLog());
        wrapLogCheck.setSelected(settings.isWrapLogText());
    }
    
    private AppSettings collectSettings() {
        AppSettings settings = new AppSettings();
        
        // TRACE32
        String trace32Path = trace32PathField.getText().trim();
        settings.setTrace32ExecutablePath(trace32Path.isEmpty() ? null : trace32Path);
        settings.setConnectionTimeout(timeoutSpinner.getValue());
        
        // Flashing
        String defaultProfile = defaultProfileCombo.getValue();
        settings.setDefaultTargetProfile(defaultProfile == null || defaultProfile.isEmpty() ? 
            null : defaultProfile);
        settings.setAutoValidate(autoValidateCheck.isSelected());
        settings.setConfirmBeforeFlash(confirmFlashCheck.isSelected());
        settings.setConfirmBeforeAbort(confirmAbortCheck.isSelected());
        
        // Output
        String outputDir = outputDirField.getText().trim();
        settings.setDefaultOutputDirectory(outputDir.isEmpty() ? null : outputDir);
        settings.setAutoGenerateFilename(autoFilenameCheck.isSelected());
        settings.setOpenOutputFolderAfterGenerate(openFolderCheck.isSelected());
        
        // Appearance
        settings.setTheme(themeCombo.getValue());
        settings.setLogMaxLines(logLinesSpinner.getValue());
        settings.setShowTimestampInLog(showTimestampCheck.isSelected());
        settings.setWrapLogText(wrapLogCheck.isSelected());
        
        return settings;
    }
    
    @FXML
    private void handleBrowseTrace32() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select TRACE32 Executable");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Executable Files", "*.exe"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Start from current path if set
        String currentPath = trace32PathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }
        
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            trace32PathField.setText(file.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleBrowseOutputDir() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Output Directory");
        
        // Start from current path if set
        String currentPath = outputDirField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                dirChooser.setInitialDirectory(currentDir);
            }
        }
        
        File dir = dirChooser.showDialog(getStage());
        if (dir != null) {
            outputDirField.setText(dir.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleResetDefaults() {
        boolean confirmed = Alerts.confirm(
            "Reset to Defaults", 
            "Are you sure you want to reset all settings to their default values?"
        );
        
        if (confirmed) {
            settingsStore.resetToDefaults();
            loadSettings();
            log.info("Settings reset to defaults");
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            AppSettings settings = collectSettings();
            settingsStore.save(settings);
            
            // Apply theme change immediately
            ThemeManager.getInstance().applyThemeGlobally(settings.getTheme());
            
            log.info("Settings saved successfully");
            Alerts.info("Settings Saved", "Your settings have been saved successfully.");
            closeDialog();
        } catch (Exception e) {
            log.error("Failed to save settings", e);
            Alerts.error("Save Failed", "Failed to save settings: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        // Check if there are unsaved changes
        AppSettings currentSettings = collectSettings();
        if (!settingsEqual(currentSettings, originalSettings)) {
            boolean confirmed = Alerts.confirm(
                "Unsaved Changes",
                "You have unsaved changes. Are you sure you want to discard them?"
            );
            if (!confirmed) {
                return;
            }
        }
        closeDialog();
    }
    
    private boolean settingsEqual(AppSettings a, AppSettings b) {
        // Simple equality check for key fields
        return nullSafeEquals(a.getTrace32ExecutablePath(), b.getTrace32ExecutablePath()) &&
               a.getConnectionTimeout() == b.getConnectionTimeout() &&
               nullSafeEquals(a.getDefaultTargetProfile(), b.getDefaultTargetProfile()) &&
               a.isAutoValidate() == b.isAutoValidate() &&
               a.isConfirmBeforeFlash() == b.isConfirmBeforeFlash() &&
               a.isConfirmBeforeAbort() == b.isConfirmBeforeAbort() &&
               nullSafeEquals(a.getDefaultOutputDirectory(), b.getDefaultOutputDirectory()) &&
               a.isAutoGenerateFilename() == b.isAutoGenerateFilename() &&
               a.isOpenOutputFolderAfterGenerate() == b.isOpenOutputFolderAfterGenerate() &&
               nullSafeEquals(a.getTheme(), b.getTheme()) &&
               a.getLogMaxLines() == b.getLogMaxLines() &&
               a.isShowTimestampInLog() == b.isShowTimestampInLog() &&
               a.isWrapLogText() == b.isWrapLogText();
    }
    
    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    private Stage getStage() {
        if (settingsTabs.getScene() != null && 
            settingsTabs.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        throw new IllegalStateException("SettingsController not attached to a Stage");
    }
    
    private void closeDialog() {
        getStage().close();
    }
}
