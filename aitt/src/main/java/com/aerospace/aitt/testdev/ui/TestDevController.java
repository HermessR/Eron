package com.aerospace.aitt.testdev.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.aerospace.aitt.backend.hardware.CMMScriptGenerator;
import com.aerospace.aitt.backend.hardware.MPCHardwareImpl;
import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.testdev.service.CMMTestService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;

/**
 * Controller for CMM Test Development Module.
 * 
 * Provides UI for:
 * - Defining test inputs (logical names, types, values)
 * - Defining expected outputs
 * - Generating Trace32 CMM scripts
 * - Executing scripts and monitoring results
 */
@SuppressWarnings("unused") // FXML handlers invoked via reflection
public class TestDevController extends BaseController {
    
    private static final AppLogger log = new AppLogger(TestDevController.class);
    
    // ==================== SERVICES ====================
    
    private CMMTestService cmmTestService;
    
    // ==================== FXML CONTROLS ====================
    
    // Header
    @FXML private Label lblChipsetType;
    @FXML private Label lblTestStatus;
    
    // Chipset Selection
    @FXML private ComboBox<String> chipsetCombo;
    
    // Progress
    @FXML private StackPane progressPane;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgressStatus;
    @FXML private Button btnCancel;
    
    // Test Definition Tab
    @FXML private TextField txtTestName;
    @FXML private TextField txtBreakpointName;
    @FXML private TableView<TestInputRow> inputTable;
    @FXML private TableView<TestOutputRow> outputTable;
    @FXML private Button btnAddInput;
    @FXML private Button btnAddOutput;
    @FXML private Button btnSyncInputs;
    @FXML private Button btnDeleteInput;
    @FXML private Button btnDeleteOutput;
    
    // Generate & Execute Tab
    @FXML private Label lblInputCount;
    @FXML private Label lblOutputCount;
    @FXML private Label lblChipset;
    @FXML private TextArea txtScriptPreview;
    @FXML private Button btnGenerateScript;
    @FXML private Button btnExecuteScript;
    
    // Results & Logs Tab
    @FXML private Label lblGeneratedScript;
    @FXML private Label lblExecutionLog;
    @FXML private TextArea executionLog;
    @FXML private Button btnClearLog;
    @FXML private Button btnRefreshLog;
    @FXML private Button btnOpenLogFile;
    
    // Status
    @FXML private Label statusLabel;
    @FXML private Label lblTimestamp;
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void initialize() {
        try {
            log.info("Initializing TestDevController");
            
            // Initialize service
            cmmTestService = new CMMTestService(new MPCHardwareImpl());
            
            // Setup UI
            setupUI();
            setupTables();
            
            lblChipsetType.setText("Chipset: " + cmmTestService.getChipsetType());
            lblChipset.setText(cmmTestService.getChipsetType());
            
            log.info("TestDevController initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize TestDevController: " + e.getMessage(), e);
            Alerts.error("Initialization Error", "Failed to initialize CMM Test Module: " + e.getMessage());
        }
    }
    
    @Override
    public void onShow() {
        log.info("CMM Test Development module displayed");
        updateStatus("Module ready", false);
    }
    
    // ==================== UI SETUP ====================
    
    private void setupUI() {
        // Setup chipset selector
        chipsetCombo.setItems(FXCollections.observableArrayList("MPC5777M", "S32K148"));
        chipsetCombo.setValue("MPC5777M");
        chipsetCombo.setOnAction(e -> handleChipsetChange());
        
        txtTestName.setText("ChipsetValidation_" + System.currentTimeMillis());
        txtBreakpointName.setText("UserFunction_ValidateChipset");
    }
    
    private void setupTables() {
        // Input Table Columns
        TableColumn<TestInputRow, String> inputNameCol = new TableColumn<>("Logical Name");
        inputNameCol.setCellValueFactory(new PropertyValueFactory<>("logicalName"));
        inputNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        inputNameCol.setOnEditCommit(event -> {
            event.getRowValue().setLogicalName(event.getNewValue());
            updateResolvedValues(event.getRowValue());
        });
        inputNameCol.setPrefWidth(150);
        
        TableColumn<TestInputRow, String> inputTypeCol = new TableColumn<>("Data Type");
        inputTypeCol.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        inputTypeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(
                "boolean",
                "int8 (signed)", "int8 (unsigned)",
                "int16 (signed)", "int16 (unsigned)",
                "int32 (signed)", "int32 (unsigned)",
                "float32", "float64"
            )
        ));
        inputTypeCol.setPrefWidth(120);
        
        TableColumn<TestInputRow, String> inputModeCol = new TableColumn<>("Mode");
        inputModeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
        inputModeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList("R", "W", "RW")
        ));
        inputModeCol.setPrefWidth(80);
        
        TableColumn<TestInputRow, String> inputValueCol = new TableColumn<>("Test Value");
        inputValueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        inputValueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        inputValueCol.setPrefWidth(120);
        
        TableColumn<TestInputRow, String> inputAddrCol = new TableColumn<>("Address");
        inputAddrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        inputAddrCol.setCellFactory(TextFieldTableCell.forTableColumn());
        inputAddrCol.setPrefWidth(120);
        
        TableColumn<TestInputRow, String> inputFormatCol = new TableColumn<>("Format");
        inputFormatCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        inputFormatCol.setCellFactory(TextFieldTableCell.forTableColumn());
        inputFormatCol.setPrefWidth(100);
        
        inputTable.getColumns().addAll(inputNameCol, inputTypeCol, inputModeCol, inputValueCol, inputAddrCol, inputFormatCol);
        inputTable.setEditable(true);
        inputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Output Table Columns
        TableColumn<TestOutputRow, String> outputNameCol = new TableColumn<>("Logical Name");
        outputNameCol.setCellValueFactory(new PropertyValueFactory<>("logicalName"));
        outputNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        outputNameCol.setPrefWidth(150);
        
        TableColumn<TestOutputRow, String> outputTypeCol = new TableColumn<>("Data Type");
        outputTypeCol.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        outputTypeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(
                "boolean",
                "int8 (signed)", "int8 (unsigned)",
                "int16 (signed)", "int16 (unsigned)",
                "int32 (signed)", "int32 (unsigned)",
                "float32", "float64"
            )
        ));
        outputTypeCol.setPrefWidth(120);
        
        TableColumn<TestOutputRow, String> outputModeCol = new TableColumn<>("Mode");
        outputModeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
        outputModeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList("R", "W", "RW")
        ));
        outputModeCol.setPrefWidth(80);
        
        TableColumn<TestOutputRow, String> outputExpectedCol = new TableColumn<>("Expected Value");
        outputExpectedCol.setCellValueFactory(new PropertyValueFactory<>("expectedValue"));
        outputExpectedCol.setCellFactory(TextFieldTableCell.forTableColumn());
        outputExpectedCol.setPrefWidth(120);
        
        outputTable.getColumns().addAll(outputNameCol, outputTypeCol, outputModeCol, outputExpectedCol);
        outputTable.setEditable(true);
        outputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    // ==================== INPUT/OUTPUT MANAGEMENT ====================
    
    @FXML
    private void handleAddInput() {
        inputTable.getItems().add(new TestInputRow());
        updateSummary();
    }
    
    @FXML
    private void handleAddOutput() {
        // Get the selected input row to auto-fill logical name
        TestInputRow selectedInput = inputTable.getSelectionModel().getSelectedItem();
        
        TestOutputRow newOutput = new TestOutputRow();
        if (selectedInput != null && selectedInput.getLogicalName() != null && !selectedInput.getLogicalName().isEmpty()) {
            // Pre-populate with the selected input's logical name
            newOutput.setLogicalName(selectedInput.getLogicalName());
        }
        
        outputTable.getItems().add(newOutput);
        updateSummary();
        
        if (selectedInput != null && selectedInput.getLogicalName() != null && !selectedInput.getLogicalName().isEmpty()) {
            updateStatus("Output row added with logical name: " + selectedInput.getLogicalName(), false);
        }
    }
    
    @FXML
    private void handleDeleteInput() {
        TestInputRow selected = inputTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            inputTable.getItems().remove(selected);
            updateSummary();
            updateStatus("Input row deleted", false);
        } else {
            Alerts.warning("Delete Input", "Please select a row to delete.");
        }
    }
    
    @FXML
    private void handleDeleteOutput() {
        TestOutputRow selected = outputTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            outputTable.getItems().remove(selected);
            updateSummary();
            updateStatus("Output row deleted", false);
        } else {
            Alerts.warning("Delete Output", "Please select a row to delete.");
        }
    }
    
    @FXML
    private void handleSyncInputs() {
        if (inputTable.getItems().isEmpty()) {
            Alerts.warning("Sync Inputs", "No input rows found. Add inputs first.");
            return;
        }
        
        // Clear existing outputs
        outputTable.getItems().clear();
        
        // Create output rows for each input with matching logical name
        int syncCount = 0;
        for (TestInputRow input : inputTable.getItems()) {
            if (input.getLogicalName() != null && !input.getLogicalName().isEmpty()) {
                TestOutputRow output = new TestOutputRow();
                output.setLogicalName(input.getLogicalName());
                // Match data type if possible (for consistency)
                output.setDataType(input.getDataType());
                outputTable.getItems().add(output);
                syncCount++;
            }
        }
        
        updateSummary();
        if (syncCount > 0) {
            updateStatus("Synced " + syncCount + " input logical names to outputs", false);
            Alerts.info("Sync Complete", "Created " + syncCount + " output rows with matching logical names.");
        } else {
            Alerts.warning("Sync Result", "No inputs with logical names found to sync.");
        }
    }
    
    @FXML
    private void handleChipsetChange() {
        String selectedChipset = chipsetCombo.getValue();
        if (selectedChipset == null || selectedChipset.isEmpty()) {
            return;
        }
        
        try {
            if (selectedChipset.equals("MPC5777M")) {
                cmmTestService = new CMMTestService(new MPCHardwareImpl());
            } else if (selectedChipset.equals("S32K148")) {
                // For now, use MPC implementation as placeholder
                // TODO: Create S32K148HardwareImpl
                cmmTestService = new CMMTestService(new MPCHardwareImpl());
                Alerts.warning("Chipset", "S32K148 support coming soon. Using MPC5777M.");
            }
            
            lblChipsetType.setText("Chipset: " + selectedChipset);
            lblChipset.setText(selectedChipset);
            updateStatus("Chipset switched to " + selectedChipset, false);
            log.info("Switched to chipset: " + selectedChipset);
        } catch (Exception e) {
            log.error("Failed to switch chipset: " + e.getMessage(), e);
            Alerts.error("Chipset Error", "Failed to switch chipset: " + e.getMessage());
        }
    }
    
    private void updateResolvedValues(TestInputRow row) {
        try {
            if (row.getLogicalName() != null && !row.getLogicalName().isEmpty()) {
                long addr = cmmTestService.getResolvedAddress(row.getLogicalName());
                String fmt = cmmTestService.getResolvedFormat(row.getLogicalName());
                row.setAddress(String.format("0x%08X", addr));
                row.setFormat(fmt);
                inputTable.refresh();
            }
        } catch (IOException e) {
            log.warn("Failed to resolve address: " + e.getMessage());
        }
    }
    
    private void updateSummary() {
        lblInputCount.setText(inputTable.getItems().size() + " defined");
        lblOutputCount.setText(outputTable.getItems().size() + " defined");
    }
    
    // ==================== SCRIPT GENERATION ====================
    
    @FXML
    private void handleGenerateScript() {
        try {
            // Validate inputs
            if (inputTable.getItems().isEmpty()) {
                Alerts.warning("Validation Error", "At least one input must be defined");
                return;
            }
            
            if (txtTestName.getText() == null || txtTestName.getText().trim().isEmpty()) {
                Alerts.warning("Validation Error", "Test name is required");
                return;
            }
            
            if (txtBreakpointName.getText() == null || txtBreakpointName.getText().trim().isEmpty()) {
                Alerts.warning("Validation Error", "Breakpoint name is required");
                return;
            }
            
            // Show progress
            showProgress(true, "Generating CMM script...");
            updateStatus("Generating script...", true);
            
            // Convert UI data to service objects
            List<CMMScriptGenerator.TestInput> inputs = new ArrayList<>();
            for (TestInputRow row : inputTable.getItems()) {
                inputs.add(new CMMScriptGenerator.TestInput(
                    row.getLogicalName(),
                    row.getDataType(),
                    row.getMode(),
                    row.getValue()
                ));
            }
            
            List<CMMScriptGenerator.TestOutput> outputs = new ArrayList<>();
            for (TestOutputRow row : outputTable.getItems()) {
                outputs.add(new CMMScriptGenerator.TestOutput(
                    row.getLogicalName(),
                    row.getDataType(),
                    row.getMode()
                ));
            }
            
            // Async generation
            cmmTestService.generateTestScript(
                txtTestName.getText(),
                inputs,
                outputs,
                txtBreakpointName.getText()
            ).thenAccept(scriptPath -> {
                Platform.runLater(() -> {
                    handleGenerateScriptSuccess(scriptPath);
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    handleGenerateScriptError(ex);
                });
                return null;
            });
            
        } catch (Exception e) {
            log.error("Script generation failed: " + e.getMessage(), e);
            Alerts.error("Generation Error", "Failed to generate script: " + e.getMessage());
            showProgress(false, null);
            updateStatus("Generation failed", false);
        }
    }
    
    private void handleGenerateScriptSuccess(Path scriptPath) {
        try {
            showProgress(false, null);
            
            lblGeneratedScript.setText(scriptPath.getFileName().toString());
            
            // Read and display preview
            String scriptContent = new String(Files.readAllBytes(scriptPath), StandardCharsets.UTF_8);
            String preview = scriptContent.length() > 2000 
                ? scriptContent.substring(0, 2000) + "\n... (script truncated for preview)"
                : scriptContent;
            txtScriptPreview.setText(preview);
            
            btnExecuteScript.setDisable(false);
            
            updateStatus("Script generated successfully", false);
            Alerts.info("Success", "CMM script generated: " + scriptPath.getFileName());
            
            log.info("Script generated: " + scriptPath);
            
        } catch (Exception e) {
            log.error("Failed to display script: " + e.getMessage(), e);
            Alerts.error("Display Error", e.getMessage());
        }
    }
    
    private void handleGenerateScriptError(Throwable ex) {
        showProgress(false, null);
        Alerts.error("Generation Error", "Script generation failed: " + ex.getMessage());
        updateStatus("Generation failed", false);
        log.error("Script generation error: " + ex.getMessage(), ex);
    }
    
    // ==================== SCRIPT EXECUTION ====================
    
    @FXML
    private void handleExecuteScript() {
        Path scriptPath = cmmTestService.getLastGeneratedScript();
        
        if (scriptPath == null || !Files.exists(scriptPath)) {
            Alerts.warning("Execution Error", "No script available. Generate a script first.");
            return;
        }
        
        if (!Alerts.confirm("Execute Script?", 
            "Execute CMM script on Trace32?\n" + scriptPath.getFileName())) {
            return;
        }
        
        showProgress(true, "Executing CMM script on Trace32...");
        updateStatus("Executing on Trace32...", true);
        
        cmmTestService.executeScript(scriptPath)
            .thenAccept(logPath -> {
                Platform.runLater(() -> {
                    handleExecuteScriptSuccess(logPath);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    handleExecuteScriptError(ex);
                });
                return null;
            });
    }
    
    private void handleExecuteScriptSuccess(Path logPath) {
        try {
            showProgress(false, null);
            
            lblExecutionLog.setText(logPath.getFileName().toString());
            
            // Read and display log
            if (Files.exists(logPath)) {
                String logContent = new String(Files.readAllBytes(logPath), StandardCharsets.UTF_8);
                executionLog.setText(logContent);
            }
            
            updateStatus("Execution complete", false);
            Alerts.info("Success", "CMM script executed successfully.\nCheck Results & Logs tab for details.");
            
            log.info("Script execution complete: " + logPath);
            
        } catch (Exception e) {
            log.error("Failed to read execution log: " + e.getMessage(), e);
            Alerts.error("Log Error", e.getMessage());
        }
    }
    
    private void handleExecuteScriptError(Throwable ex) {
        showProgress(false, null);
        Alerts.error("Execution Error", "Script execution failed: " + ex.getMessage());
        updateStatus("Execution failed", false);
        log.error("Script execution error: " + ex.getMessage(), ex);
    }
    
    // ==================== LOG MANAGEMENT ====================
    
    @FXML
    private void handleClearLog() {
        executionLog.clear();
        lblExecutionLog.setText("(Cleared)");
    }
    
    @FXML
    private void handleRefreshLog() {
        Path logPath = cmmTestService.getLastExecutionLog();
        if (logPath != null && Files.exists(logPath)) {
            try {
                String logContent = new String(Files.readAllBytes(logPath), StandardCharsets.UTF_8);
                executionLog.setText(logContent);
                updateStatus("Log refreshed", false);
            } catch (IOException e) {
                Alerts.error("Read Error", "Failed to refresh log: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleOpenLogFile() {
        Path logPath = cmmTestService.getLastExecutionLog();
        if (logPath != null && Files.exists(logPath)) {
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("notepad", logPath.toString()).start();
                } else {
                    new ProcessBuilder("xdg-open", logPath.toString()).start();
                }
            } catch (IOException e) {
                Alerts.error("Open Error", "Failed to open log file: " + e.getMessage());
            }
        }
    }
    
    // ==================== UI HELPERS ====================
    
    private void showProgress(boolean show, String message) {
        progressPane.setVisible(show);
        progressPane.setManaged(show);
        if (message != null) {
            lblProgressStatus.setText(message);
            progressBar.setProgress(-1); // Indeterminate
        }
    }
    
    private void updateStatus(String message, boolean working) {
        statusLabel.setText(message);
        lblTimestamp.setText(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
    
    @FXML
    private void handleCancel() {
        showProgress(false, null);
        updateStatus("Cancelled", false);
    }
    
    @FXML
    private void handleBack() {
        log.info("Returning to dashboard from CMM Test Development module");
        navigateTo("dashboard");
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Model for test input table row.
     */
    public static class TestInputRow {
        private String logicalName = "";
        private String dataType = "int32 (signed)";
        private String mode = "RW";
        private String value = "";
        private String address = "";
        private String format = "";
        
        public String getLogicalName() { return logicalName; }
        public void setLogicalName(String v) { this.logicalName = v; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String v) { this.dataType = v; }
        
        public String getMode() { return mode; }
        public void setMode(String v) { this.mode = v; }
        
        public String getValue() { return value; }
        public void setValue(String v) { this.value = v; }
        
        public String getAddress() { return address; }
        public void setAddress(String v) { this.address = v; }
        
        public String getFormat() { return format; }
        public void setFormat(String v) { this.format = v; }
    }
    
    /**
     * Model for test output table row.
     */
    public static class TestOutputRow {
        private String logicalName = "";
        private String dataType = "int32 (unsigned)";
        private String mode = "R";
        private String expectedValue = "";
        
        public String getLogicalName() { return logicalName; }
        public void setLogicalName(String v) { this.logicalName = v; }
        
        public String getDataType() { return dataType; }
        public void setDataType(String v) { this.dataType = v; }
        
        public String getMode() { return mode; }
        public void setMode(String v) { this.mode = v; }
        
        public String getExpectedValue() { return expectedValue; }
        public void setExpectedValue(String v) { this.expectedValue = v; }
    }
}
