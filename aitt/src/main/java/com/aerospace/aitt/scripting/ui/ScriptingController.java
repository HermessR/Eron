package com.aerospace.aitt.scripting.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aerospace.aitt.core.config.SettingsStore;
import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.native_.CommandResult;
import com.aerospace.aitt.native_.HardwareBridge;
import com.aerospace.aitt.native_.HardwareService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

/**
 * Controller for the CMM Scripting Console.
 * Allows users to write, edit, load, save, and execute custom CMM scripts.
 */
@SuppressWarnings("unused") // FXML handlers and fields are invoked via reflection
public class ScriptingController extends BaseController {
    
    private static final AppLogger log = new AppLogger(ScriptingController.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    // ==================== FXML CONTROLS ====================
    
    // Header
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblScriptName;
    
    // Loading
    @FXML private StackPane loadingPane;
    @FXML private ProgressBar mainProgressBar;
    @FXML private Label lblLoadingStatus;
    @FXML private Button btnCancelExecution;
    
    // Editor
    @FXML private TextArea scriptEditor;
    @FXML private Label lblLineCount;
    @FXML private Label lblCursorPos;
    
    // Commands
    @FXML private TextField txtQuickCommand;
    @FXML private Button btnSendCommand;
    @FXML private ComboBox<String> cmbRecentCommands;
    
    // Quick Actions
    @FXML private Button btnSystemUp;
    @FXML private Button btnSystemReset;
    @FXML private Button btnBreak;
    @FXML private Button btnGo;
    
    // Script Actions
    @FXML private Button btnNewScript;
    @FXML private Button btnOpenScript;
    @FXML private Button btnSaveScript;
    @FXML private Button btnRunScript;
    @FXML private Button btnStopScript;
    
    // Output
    @FXML private TextArea outputArea;
    @FXML private Button btnClearOutput;
    
    // History
    @FXML private ListView<String> historyList;
    @FXML private Button btnClearHistory;
    
    // ==================== STATE ====================
    
    private HardwareService hardwareService;
    private Path currentScriptPath;
    private boolean scriptModified = false;
    private final AtomicBoolean scriptRunning = new AtomicBoolean(false);
    private CompletableFuture<Void> runningScript;
    
    private final ObservableList<String> commandHistory = FXCollections.observableArrayList();
    private final ObservableList<String> recentCommands = FXCollections.observableArrayList();
    
    // ==================== INITIALIZATION ====================
    
    @Override
    public void initialize() {
        log.info("Initializing ScriptingController");
        
        // Setup editor
        setupEditor();
        setupQuickCommand();
        setupHistory();
        
        // Initialize services async
        CompletableFuture.supplyAsync(() -> {
            try {
                if (HardwareBridge.isNativeAvailable()) {
                    return new HardwareService();
                }
            } catch (Exception e) {
                log.error("Failed to initialize hardware service", e);
            }
            return null;
        }).thenAcceptAsync(service -> {
            hardwareService = service;
            updateConnectionStatus();
            if (service == null) {
                appendOutput("[WARN] Native library not available. Script execution disabled.");
                disableExecutionControls();
            }
        }, Platform::runLater);
        
        // Load sample script
        loadSampleScript();
        
        log.info("ScriptingController initialized");
    }
    
    private void setupEditor() {
        // Track modifications
        scriptEditor.textProperty().addListener((obs, oldVal, newVal) -> {
            scriptModified = true;
            updateLineCount();
        });
        
        // Track cursor position
        scriptEditor.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            updateCursorPosition();
        });
        
        // Set monospace font via style
        scriptEditor.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");
        outputArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        
        updateLineCount();
    }
    
    private void setupQuickCommand() {
        txtQuickCommand.setOnAction(e -> handleSendCommand());
        
        // Ctrl+Enter to send
        txtQuickCommand.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP && !recentCommands.isEmpty()) {
                int idx = recentCommands.indexOf(txtQuickCommand.getText());
                if (idx > 0) {
                    txtQuickCommand.setText(recentCommands.get(idx - 1));
                } else if (idx == -1) {
                    txtQuickCommand.setText(recentCommands.get(recentCommands.size() - 1));
                }
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN && !recentCommands.isEmpty()) {
                int idx = recentCommands.indexOf(txtQuickCommand.getText());
                if (idx >= 0 && idx < recentCommands.size() - 1) {
                    txtQuickCommand.setText(recentCommands.get(idx + 1));
                }
                e.consume();
            }
        });
        
        if (cmbRecentCommands != null) {
            cmbRecentCommands.setItems(recentCommands);
            cmbRecentCommands.setOnAction(e -> {
                String cmd = cmbRecentCommands.getValue();
                if (cmd != null && !cmd.isEmpty()) {
                    txtQuickCommand.setText(cmd);
                    txtQuickCommand.requestFocus();
                }
            });
        }
    }
    
    private void setupHistory() {
        historyList.setItems(commandHistory);
        historyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Extract command from history entry
                    int colonIdx = selected.indexOf("] ");
                    if (colonIdx > 0) {
                        String cmd = selected.substring(colonIdx + 2);
                        if (cmd.startsWith(">> ")) {
                            txtQuickCommand.setText(cmd.substring(3));
                            txtQuickCommand.requestFocus();
                        }
                    }
                }
            }
        });
    }
    
    private void loadSampleScript() {
        String sample = """
            ; ============================================================================
            ; Sample CMM Script
            ; ============================================================================
            ; This is a template for TRACE32 commands
            
            ; System Initialization
            SYStem.RESet
            SYStem.CPU CortexM4
            SYStem.Up
            
            ; Wait for target to stabilize
            WAIT 100.ms
            
            ; Display system information
            SYStem.state
            
            ; Memory operations example
            ; Data.Dump 0x08000000
            ; Data.Set 0x20000000 %Long 0x12345678
            
            ; Flash operations example
            ; FLASH.RESet
            ; FLASH.Create 1. 0x08000000--0x081FFFFF
            ; FLASH.Program.ALL
            
            ENDDO
            """;
        scriptEditor.setText(sample);
        scriptModified = false;
        currentScriptPath = null;
        updateScriptName("Untitled.cmm");
    }
    
    private void updateLineCount() {
        if (lblLineCount != null) {
            String text = scriptEditor.getText();
            int lines = text.isEmpty() ? 1 : text.split("\n", -1).length;
            lblLineCount.setText("Lines: " + lines);
        }
    }
    
    private void updateCursorPosition() {
        if (lblCursorPos != null) {
            int pos = scriptEditor.getCaretPosition();
            String text = scriptEditor.getText();
            
            int line = 1;
            int col = 1;
            for (int i = 0; i < pos && i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
            }
            
            lblCursorPos.setText("Ln " + line + ", Col " + col);
        }
    }
    
    private void updateScriptName(String name) {
        if (lblScriptName != null) {
            lblScriptName.setText(name + (scriptModified ? " *" : ""));
        }
    }
    
    private void updateConnectionStatus() {
        if (lblConnectionStatus != null) {
            if (hardwareService != null && hardwareService.isConnected()) {
                lblConnectionStatus.setText("Connected");
                lblConnectionStatus.setStyle("-fx-text-fill: #00ff88;");
            } else if (hardwareService != null) {
                lblConnectionStatus.setText("Disconnected");
                lblConnectionStatus.setStyle("-fx-text-fill: #ffaa00;");
            } else {
                lblConnectionStatus.setText("Native Unavailable");
                lblConnectionStatus.setStyle("-fx-text-fill: #ff4444;");
            }
        }
    }
    
    private void disableExecutionControls() {
        if (btnRunScript != null) btnRunScript.setDisable(true);
        if (btnSendCommand != null) btnSendCommand.setDisable(true);
        if (btnSystemUp != null) btnSystemUp.setDisable(true);
        if (btnSystemReset != null) btnSystemReset.setDisable(true);
        if (btnBreak != null) btnBreak.setDisable(true);
        if (btnGo != null) btnGo.setDisable(true);
    }
    
    // ==================== NAVIGATION ====================
    
    @FXML
    private void handleBack() {
        if (scriptModified) {
            if (!Alerts.confirm("Unsaved Changes", "You have unsaved changes. Discard and return to dashboard?")) {
                return;
            }
        }
        if (scriptRunning.get()) {
            handleStopScript();
        }
        navigateTo("dashboard");
    }
    
    // ==================== SCRIPT ACTIONS ====================
    
    @FXML
    private void handleNewScript() {
        if (scriptModified) {
            if (!Alerts.confirm("Unsaved Changes", "Create new script? Unsaved changes will be lost.")) {
                return;
            }
        }
        loadSampleScript();
        appendOutput("[INFO] New script created");
    }
    
    @FXML
    private void handleOpenScript() {
        if (scriptModified) {
            if (!Alerts.confirm("Unsaved Changes", "Open script? Unsaved changes will be lost.")) {
                return;
            }
        }
        
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open CMM Script");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CMM Scripts", "*.cmm"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Set initial directory to output folder if exists
        String outputDirStr = SettingsStore.getInstance().getSettings().getDefaultOutputDirectory();
        if (outputDirStr != null && !outputDirStr.isEmpty()) {
            Path outputDir = Path.of(outputDirStr);
            if (Files.isDirectory(outputDir)) {
                chooser.setInitialDirectory(outputDir.toFile());
            }
        }
        
        File file = chooser.showOpenDialog(getWindow());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                scriptEditor.setText(content);
                currentScriptPath = file.toPath();
                scriptModified = false;
                updateScriptName(file.getName());
                appendOutput("[INFO] Opened: " + file.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to open script", e);
                Alerts.error("Failed to open script: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleSaveScript() {
        if (currentScriptPath == null) {
            handleSaveScriptAs();
            return;
        }
        
        try {
            Files.writeString(currentScriptPath, scriptEditor.getText());
            scriptModified = false;
            updateScriptName(currentScriptPath.getFileName().toString());
            appendOutput("[INFO] Saved: " + currentScriptPath);
        } catch (IOException e) {
            log.error("Failed to save script", e);
            Alerts.error("Failed to save script: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSaveScriptAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CMM Script");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CMM Scripts", "*.cmm")
        );
        chooser.setInitialFileName("script.cmm");
        
        String outputDirStr2 = SettingsStore.getInstance().getSettings().getDefaultOutputDirectory();
        if (outputDirStr2 != null && !outputDirStr2.isEmpty()) {
            Path outputDir = Path.of(outputDirStr2);
            if (Files.isDirectory(outputDir)) {
                chooser.setInitialDirectory(outputDir.toFile());
            }
        }
        
        File file = chooser.showSaveDialog(getWindow());
        if (file != null) {
            try {
                Path path = file.toPath();
                if (!path.toString().endsWith(".cmm")) {
                    path = Path.of(path.toString() + ".cmm");
                }
                Files.writeString(path, scriptEditor.getText());
                currentScriptPath = path;
                scriptModified = false;
                updateScriptName(path.getFileName().toString());
                appendOutput("[INFO] Saved: " + path);
            } catch (IOException e) {
                log.error("Failed to save script", e);
                Alerts.error("Failed to save script: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleRunScript() {
        if (hardwareService == null) {
            Alerts.error("Native library not available. Cannot execute scripts.");
            return;
        }
        
        if (!hardwareService.isConnected()) {
            Alerts.warning("Not Connected", "Connect to a debug probe before running scripts.");
            return;
        }
        
        if (scriptRunning.get()) {
            Alerts.info("Script already running");
            return;
        }
        
        String script = scriptEditor.getText();
        if (script.trim().isEmpty()) {
            Alerts.warning("Empty Script", "No commands to execute.");
            return;
        }
        
        scriptRunning.set(true);
        showLoading("Executing script...");
        appendOutput("\n[SCRIPT START] " + LocalDateTime.now().format(TIME_FORMAT));
        
        runningScript = CompletableFuture.runAsync(() -> {
            String[] lines = script.split("\n");
            int lineNum = 0;
            
            for (String line : lines) {
                if (!scriptRunning.get()) {
                    Platform.runLater(() -> appendOutput("[SCRIPT ABORTED]"));
                    break;
                }
                
                lineNum++;
                String trimmed = line.trim();
                
                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith(";")) {
                    continue;
                }
                
                // Execute command
                final int currentLine = lineNum;
                final String cmd = trimmed;
                
                Platform.runLater(() -> updateLoadingStatus("Line " + currentLine + ": " + cmd));
                
                try {
                    CommandResult result = hardwareService.sendCommandAndWait(cmd, 5000);
                    
                    Platform.runLater(() -> {
                        addToHistory(">> " + cmd);
                        if (result.success()) {
                            String resp = result.response();
                            if (resp != null && !resp.trim().isEmpty()) {
                                appendOutput(resp);
                            }
                        } else {
                            appendOutput("[ERROR] " + result.errorMessage());
                        }
                    });
                    
                    // Small delay between commands
                    Thread.sleep(50);
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        appendOutput("[ERROR Line " + currentLine + "] " + e.getMessage());
                    });
                }
            }
        }).whenComplete((v, ex) -> Platform.runLater(() -> {
            scriptRunning.set(false);
            hideLoading();
            appendOutput("[SCRIPT END] " + LocalDateTime.now().format(TIME_FORMAT) + "\n");
            if (ex != null) {
                appendOutput("[ERROR] " + ex.getMessage());
            }
        }));
    }
    
    @FXML
    private void handleStopScript() {
        if (scriptRunning.get()) {
            scriptRunning.set(false);
            if (runningScript != null) {
                runningScript.cancel(true);
            }
            appendOutput("[SCRIPT STOPPED BY USER]");
            hideLoading();
        }
    }
    
    // ==================== COMMAND ACTIONS ====================
    
    @FXML
    private void handleSendCommand() {
        String cmd = txtQuickCommand.getText().trim();
        if (cmd.isEmpty()) return;
        
        executeCommand(cmd);
        txtQuickCommand.clear();
    }
    
    @FXML
    private void handleSystemUp() {
        executeCommand("SYStem.Up");
    }
    
    @FXML
    private void handleSystemReset() {
        executeCommand("SYStem.RESet");
    }
    
    @FXML
    private void handleBreak() {
        executeCommand("Break");
    }
    
    @FXML
    private void handleGo() {
        executeCommand("Go");
    }
    
    private void executeCommand(String command) {
        if (hardwareService == null) {
            appendOutput("[ERROR] Native library not available");
            return;
        }
        
        if (!hardwareService.isConnected()) {
            appendOutput("[WARN] Not connected to probe - command may fail");
        }
        
        addToHistory(">> " + command);
        addToRecentCommands(command);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return hardwareService.sendCommandAndWait(command, 5000);
            } catch (Exception e) {
                return CommandResult.error(e.getMessage());
            }
        }).thenAcceptAsync(result -> {
            if (result.success()) {
                String resp = result.response();
                if (resp != null && !resp.trim().isEmpty()) {
                    appendOutput(resp);
                } else {
                    appendOutput("[OK]");
                }
            } else {
                appendOutput("[ERROR] " + result.errorMessage());
            }
            addToHistory("<< " + (result.success() ? "OK" : "ERROR"));
        }, Platform::runLater);
    }
    
    private void addToHistory(String entry) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        commandHistory.add(0, "[" + timestamp + "] " + entry);
        
        // Keep history limited
        while (commandHistory.size() > 100) {
            commandHistory.remove(commandHistory.size() - 1);
        }
    }
    
    private void addToRecentCommands(String cmd) {
        recentCommands.remove(cmd);
        recentCommands.add(0, cmd);
        while (recentCommands.size() > 20) {
            recentCommands.remove(recentCommands.size() - 1);
        }
    }
    
    // ==================== OUTPUT ====================
    
    private void appendOutput(String text) {
        if (outputArea != null) {
            outputArea.appendText(text + "\n");
            outputArea.setScrollTop(Double.MAX_VALUE);
        }
    }
    
    @FXML
    private void handleClearOutput() {
        outputArea.clear();
    }
    
    @FXML
    private void handleClearHistory() {
        commandHistory.clear();
    }
    
    // ==================== LOADING ====================
    
    private void showLoading(String message) {
        if (loadingPane != null) {
            loadingPane.setVisible(true);
            loadingPane.setManaged(true);
            if (lblLoadingStatus != null) {
                lblLoadingStatus.setText(message);
            }
            if (mainProgressBar != null) {
                mainProgressBar.setProgress(-1);
            }
        }
    }
    
    private void updateLoadingStatus(String message) {
        if (lblLoadingStatus != null) {
            lblLoadingStatus.setText(message);
        }
    }
    
    private void hideLoading() {
        if (loadingPane != null) {
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
        }
    }
    
    @FXML
    private void handleCancelExecution() {
        handleStopScript();
    }
    
    @Override
    public void onShow() {
        updateConnectionStatus();
        log.info("Scripting Console displayed");
    }
    
    @Override
    public void onHide() {
        if (scriptRunning.get()) {
            handleStopScript();
        }
    }
}
