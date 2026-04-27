package com.aerospace.aitt.flashing.ui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.flashing.data.ProfileStore;
import com.aerospace.aitt.flashing.data.SessionStore;
import com.aerospace.aitt.flashing.model.FlashStatus;
import com.aerospace.aitt.flashing.model.FlashingSession;
import com.aerospace.aitt.flashing.model.SoftwareModule;
import com.aerospace.aitt.flashing.model.TargetProfile;
import com.aerospace.aitt.flashing.model.ValidationResult;
import com.aerospace.aitt.flashing.service.FlashingService;
import com.aerospace.aitt.flashing.service.Validator;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

/**
 * Controller for the Flashing Module UI.
 * Manages firmware module table, validation, CMM generation, and TRACE32 execution.
 */
@SuppressWarnings("unused") // FXML handlers and fields are invoked via reflection
public class FlashingController extends BaseController {
    
    private final AppLogger log = new AppLogger(FlashingController.class);
    
    // Services
    private FlashingService flashingService;
    private SessionStore sessionStore;
    private ProfileStore profileStore;
    
    // State
    private final ObservableList<SoftwareModuleRow> moduleRows = FXCollections.observableArrayList();
    private final ObservableList<FlashingSessionRow> sessionRows = FXCollections.observableArrayList();
    private FlashingSession currentSession;
    private CompletableFuture<FlashingSession> runningTask;
    
    // FXML Controls - Firmware Table
    @FXML private TableView<SoftwareModuleRow> firmwareTable;
    @FXML private TableColumn<SoftwareModuleRow, String> colModuleName;
    @FXML private TableColumn<SoftwareModuleRow, String> colFilePath;
    @FXML private TableColumn<SoftwareModuleRow, String> colAddress;
    @FXML private TableColumn<SoftwareModuleRow, String> colSize;
    @FXML private TableColumn<SoftwareModuleRow, String> colChecksum;
    @FXML private TableColumn<SoftwareModuleRow, String> colStatus;
    
    // FXML Controls - Target Profile
    @FXML private ComboBox<TargetProfile> profileComboBox;
    @FXML private Label lblCpuType;
    @FXML private Label lblFlashRange;
    @FXML private Label lblRamAddress;
    
    // FXML Controls - Actions
    @FXML private Button btnAddModule;
    @FXML private Button btnRemoveModule;
    @FXML private Button btnValidate;
    @FXML private Button btnGenerateCmm;
    @FXML private Button btnRunTrace32;
    @FXML private Button btnAbort;
    @FXML private CheckBox chkRunAfterGenerate;
    
    // FXML Controls - Progress
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private Label lblProgress;
    
    // FXML Controls - Log Panel
    @FXML private TextArea logArea;
    @FXML private Button btnClearLog;
    
    // FXML Controls - Session History
    @FXML private TableView<FlashingSessionRow> sessionTable;
    @FXML private TableColumn<FlashingSessionRow, String> colSessionTime;
    @FXML private TableColumn<FlashingSessionRow, String> colSessionTarget;
    @FXML private TableColumn<FlashingSessionRow, Integer> colSessionModules;
    @FXML private TableColumn<FlashingSessionRow, String> colSessionStatus;
    @FXML private Button btnViewSession;
    @FXML private Button btnDeleteSession;
    @FXML private Button btnRefreshHistory;
    
    // FXML Controls - Output
    @FXML private TextField txtOutputPath;
    @FXML private Button btnBrowseOutput;
    
    @Override
    public void initialize() {
        log.info("Initializing FlashingController");
        
        // Setup UI immediately (no I/O)
        setupFirmwareTable();
        setupSessionTable();
        setupButtonHandlers();
        
        // Disable controls until services are loaded
        setControlsDisabled(true);
        updateStatus("Loading...");
        
        // Setup log listener
        AppLogger.setUiListener(this::appendLog);
        
        // Load services asynchronously to avoid blocking UI
        CompletableFuture.runAsync(() -> {
            profileStore = new ProfileStore();
            sessionStore = new SessionStore();
            flashingService = new FlashingService(sessionStore, profileStore);
        }).thenRunAsync(() -> {
            // Back to UI thread for UI updates
            setupProfileComboBox();
            refreshSessionHistory();
            updateControlState();
            setControlsDisabled(false);
            updateStatus("Ready");
            log.info("FlashingController initialized");
        }, Platform::runLater);
    }
    
    private void setControlsDisabled(boolean disabled) {
        if (btnAddModule != null) btnAddModule.setDisable(disabled);
        if (btnValidate != null) btnValidate.setDisable(disabled);
        if (btnGenerateCmm != null) btnGenerateCmm.setDisable(disabled);
        if (btnRunTrace32 != null) btnRunTrace32.setDisable(disabled);
        if (profileComboBox != null) profileComboBox.setDisable(disabled);
    }
    
    private void updateStatus(String status) {
        if (lblStatus != null) {
            lblStatus.setText(status);
        }
    }
    
    /**
     * Handles back navigation to dashboard.
     */
    @FXML
    private void handleBack() {
        if (runningTask != null) {
            if (!com.aerospace.aitt.core.ui.Alerts.confirm("A flashing operation is in progress. Abort and return to dashboard?")) {
                return;
            }
            flashingService.abort();
        }
        navigateTo("dashboard");
    }
    
    // ========== SETUP ==========
    
    private void setupFirmwareTable() {
        colModuleName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFilePath.setCellValueFactory(new PropertyValueFactory<>("filePath"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colChecksum.setCellValueFactory(new PropertyValueFactory<>("checksum"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Status column styling
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle(switch (status) {
                        case "VALID" -> "-fx-text-fill: #00ff88;";
                        case "INVALID" -> "-fx-text-fill: #ff4444;";
                        case "PENDING" -> "-fx-text-fill: #ffaa00;";
                        default -> "";
                    });
                }
            }
        });
        
        firmwareTable.setItems(moduleRows);
        @SuppressWarnings("deprecation") // Using deprecated API for JavaFX compatibility
        var resizePolicy = TableView.CONSTRAINED_RESIZE_POLICY;
        firmwareTable.setColumnResizePolicy(resizePolicy);
        firmwareTable.setPlaceholder(new Label("No firmware modules loaded. Click 'Add Module' to begin."));
    }

    private void setupSessionTable() {
        colSessionTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colSessionTarget.setCellValueFactory(new PropertyValueFactory<>("targetProfile"));
        colSessionModules.setCellValueFactory(new PropertyValueFactory<>("moduleCount"));
        colSessionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Status column styling
        colSessionStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle(switch (status) {
                        case "SUCCESS" -> "-fx-text-fill: #00ff88;";
                        case "FAILED", "ABORTED" -> "-fx-text-fill: #ff4444;";
                        default -> "-fx-text-fill: #ffaa00;";
                    });
                }
            }
        });
        
        sessionTable.setItems(sessionRows);
        @SuppressWarnings("deprecation") // Using deprecated API for JavaFX compatibility
        var sessionResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY;
        sessionTable.setColumnResizePolicy(sessionResizePolicy);
        sessionTable.setPlaceholder(new Label("No session history."));
    }
    
    private void setupProfileComboBox() {
        List<TargetProfile> profiles = profileStore.findAll();
        profileComboBox.setItems(FXCollections.observableArrayList(profiles));
        
        profileComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TargetProfile profile) {
                return profile != null ? profile.name() : "";
            }
            
            @Override
            public TargetProfile fromString(String string) {
                return null;
            }
        });
        
        profileComboBox.setOnAction(e -> updateProfileInfo());
        
        if (!profiles.isEmpty()) {
            profileComboBox.getSelectionModel().selectFirst();
            updateProfileInfo();
        }
    }
    
    private void setupButtonHandlers() {
        btnAddModule.setOnAction(e -> handleAddModule());
        btnRemoveModule.setOnAction(e -> handleRemoveModule());
        btnValidate.setOnAction(e -> handleValidate());
        btnGenerateCmm.setOnAction(e -> handleGenerateCmm());
        btnRunTrace32.setOnAction(e -> handleRunTrace32());
        btnAbort.setOnAction(e -> handleAbort());
        btnClearLog.setOnAction(e -> logArea.clear());
        btnBrowseOutput.setOnAction(e -> handleBrowseOutput());
        btnViewSession.setOnAction(e -> handleViewSession());
        btnDeleteSession.setOnAction(e -> handleDeleteSession());
        btnRefreshHistory.setOnAction(e -> refreshSessionHistory());
    }
    
    // ========== PROFILE ==========
    
    private void updateProfileInfo() {
        TargetProfile profile = profileComboBox.getValue();
        if (profile != null) {
            lblCpuType.setText(profile.cpuType());
            lblFlashRange.setText(String.format("0x%08X - 0x%08X", profile.flashStart(), profile.flashEnd()));
            lblRamAddress.setText(String.format("0x%08X", profile.ramAddress()));
        } else {
            lblCpuType.setText("-");
            lblFlashRange.setText("-");
            lblRamAddress.setText("-");
        }
    }
    
    // ========== MODULE MANAGEMENT ==========
    
    @FXML
    private void handleAddModule() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Firmware Binary");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Firmware Files", "*.bin", "*.hex", "*.s19", "*.srec", "*.elf"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        List<File> files = chooser.showOpenMultipleDialog(getWindow());
        if (files == null || files.isEmpty()) return;
        
        for (File file : files) {
            showAddModuleDialog(file);
        }
    }
    
    private void showAddModuleDialog(File file) {
        Dialog<SoftwareModuleRow> dialog = new Dialog<>();
        dialog.setTitle("Add Firmware Module");
        dialog.setHeaderText("Configure module: " + file.getName());
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField nameField = new TextField(file.getName().replaceFirst("\\.[^.]+$", ""));
        TextField addressField = new TextField("0x08000000");
        
        grid.add(new Label("Module Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Flash Address:"), 0, 1);
        grid.add(addressField, 1, 1);
        grid.add(new Label("File:"), 0, 2);
        grid.add(new Label(file.getAbsolutePath()), 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    long address = parseHexAddress(addressField.getText());
                    long size = Files.size(file.toPath());
                    String checksum = com.aerospace.aitt.core.util.ChecksumUtil.sha256(file.toPath());
                    
                    return new SoftwareModuleRow(
                        UUID.randomUUID(),
                        name,
                        file.toPath(),
                        address,
                        size,
                        checksum.substring(0, 16) + "...",
                        "PENDING"
                    );
                } catch (Exception e) {
                    Alerts.error("Invalid address format: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(row -> {
            moduleRows.add(row);
            updateControlState();
            log.info("Added module: %s at 0x%08X", row.getName(), row.getAddressValue());
        });
    }
    
    @FXML
    private void handleRemoveModule() {
        SoftwareModuleRow selected = firmwareTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            moduleRows.remove(selected);
            updateControlState();
            log.info("Removed module: %s", selected.getName());
        }
    }
    
    // ========== VALIDATION ==========
    
    @FXML
    private void handleValidate() {
        if (moduleRows.isEmpty()) {
            Alerts.warn("No modules to validate.");
            return;
        }
        
        TargetProfile profile = profileComboBox.getValue();
        if (profile == null) {
            Alerts.warn("Please select a target profile.");
            return;
        }
        
        log.info("Starting validation...");
        setStatus("Validating...");
        
        List<SoftwareModule> modules = moduleRows.stream()
            .map(SoftwareModuleRow::toSoftwareModule)
            .toList();
        
        Validator validator = new Validator();
        ValidationResult result = validator.validate(modules, profile);
        
        // Update row statuses
        Map<String, List<ValidationResult.ValidationError>> errorsByModule = new HashMap<>();
        for (var error : result.errors()) {
            errorsByModule.computeIfAbsent(error.moduleName(), k -> new ArrayList<>()).add(error);
        }
        
        for (SoftwareModuleRow row : moduleRows) {
            List<ValidationResult.ValidationError> errors = errorsByModule.get(row.getId().toString());
            if (errors == null || errors.isEmpty()) {
                row.setStatus("VALID");
            } else {
                row.setStatus("INVALID");
                for (var error : errors) {
                    log.warn("Validation error: %s - %s", row.getName(), error.message());
                }
            }
        }
        
        firmwareTable.refresh();
        
        if (result.valid()) {
            setStatus("Validation passed");
            Alerts.info("All modules validated successfully.");
        } else {
            setStatus("Validation failed");
            StringBuilder msg = new StringBuilder("Validation failed:\n\n");
            for (var error : result.errors()) {
                msg.append("• ").append(error.message()).append("\n");
            }
            Alerts.error(msg.toString());
        }
        
        updateControlState();
    }
    
    // ========== CMM GENERATION ==========
    
    @FXML
    private void handleGenerateCmm() {
        if (!validateForGeneration()) return;
        
        String outputPath = txtOutputPath.getText().trim();
        if (outputPath.isEmpty()) {
            Alerts.warn("Please specify an output script path.");
            return;
        }
        
        TargetProfile profile = profileComboBox.getValue();
        List<SoftwareModule> modules = moduleRows.stream()
            .map(SoftwareModuleRow::toSoftwareModule)
            .toList();
        
        Path output = Path.of(outputPath);
        
        setStatus("Generating CMM script...");
        setProgress(-1);
        disableControls(true);
        
        runningTask = flashingService.executeAsync(
            modules,
            profile,
            output,
            false, // runTrace32
            this::setStatus,
            this::appendLog
        );
        
        runningTask.whenComplete((session, error) -> Platform.runLater(() -> {
            runningTask = null;
            disableControls(false);
            setProgress(0);
            
            if (error != null) {
                setStatus("Generation failed");
                Alerts.error("CMM generation failed: " + error.getMessage());
            } else {
                currentSession = session;
                setStatus("CMM script generated: " + output.getFileName());
                refreshSessionHistory();
                
                if (chkRunAfterGenerate.isSelected()) {
                    handleRunTrace32();
                }
            }
        }));
    }
    
    // ========== TRACE32 EXECUTION ==========
    
    @FXML
    private void handleRunTrace32() {
        if (!validateForGeneration()) return;
        
        String outputPath = txtOutputPath.getText().trim();
        if (outputPath.isEmpty()) {
            Alerts.warn("Please specify an output script path.");
            return;
        }
        
        TargetProfile profile = profileComboBox.getValue();
        List<SoftwareModule> modules = moduleRows.stream()
            .map(SoftwareModuleRow::toSoftwareModule)
            .toList();
        
        Path output = Path.of(outputPath);
        
        setStatus("Executing TRACE32...");
        setProgress(-1);
        disableControls(true);
        
        runningTask = flashingService.executeAsync(
            modules,
            profile,
            output,
            true, // runTrace32
            this::setStatus,
            this::appendLog
        );
        
        runningTask.whenComplete((session, error) -> Platform.runLater(() -> {
            runningTask = null;
            disableControls(false);
            setProgress(0);
            
            if (error != null) {
                setStatus("Execution failed");
                Alerts.error("TRACE32 execution failed: " + error.getMessage());
            } else {
                currentSession = session;
                setStatus("Flashing " + (session.status() == FlashStatus.SUCCESS ? "completed" : "failed"));
                refreshSessionHistory();
                
                if (session.status() == FlashStatus.SUCCESS) {
                    Alerts.info("Flashing completed successfully!");
                } else {
                    Alerts.error("Flashing failed: " + session.errorMessage());
                }
            }
        }));
    }
    
    @FXML
    private void handleAbort() {
        if (runningTask != null) {
            log.warn("Aborting operation...");
            flashingService.abort();
            setStatus("Aborted");
        }
    }
    
    // ========== SESSION HISTORY ==========
    
    private void refreshSessionHistory() {
        List<FlashingSession> sessions = sessionStore.findRecent(50);
        sessionRows.clear();
        
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (FlashingSession session : sessions) {
            LocalDateTime ldt = LocalDateTime.ofInstant(session.timestamp(), ZoneId.systemDefault());
            sessionRows.add(new FlashingSessionRow(
                session.id(),
                ldt.format(fmt),
                session.targetProfile().name(),
                session.modules().size(),
                session.status().name()
            ));
        }
    }
    
    @FXML
    private void handleViewSession() {
        FlashingSessionRow selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("Please select a session to view.");
            return;
        }
        
        sessionStore.findById(selected.getId()).ifPresent(session -> {
            StringBuilder details = new StringBuilder();
            details.append("Session: ").append(session.id()).append("\n");
            details.append("Target: ").append(session.targetProfile().name()).append("\n");
            details.append("Status: ").append(session.status()).append("\n");
            details.append("Time: ").append(session.timestamp()).append("\n\n");
            details.append("Modules:\n");
            for (SoftwareModule mod : session.modules()) {
                details.append("  • ").append(mod.name())
                       .append(" @ 0x").append(Long.toHexString(mod.flashAddress()).toUpperCase())
                       .append("\n");
            }
            details.append("\nLogs:\n");
            for (String logLine : session.logs()) {
                details.append(logLine).append("\n");
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Details");
            alert.setHeaderText("Session " + session.id().toString().substring(0, 8));
            
            TextArea area = new TextArea(details.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setPrefSize(600, 400);
            
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        });
    }
    
    @FXML
    private void handleDeleteSession() {
        FlashingSessionRow selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("Please select a session to delete.");
            return;
        }
        
        if (Alerts.confirm("Delete session from history?")) {
            sessionStore.delete(selected.getId());
            refreshSessionHistory();
        }
    }
    
    // ========== OUTPUT PATH ==========
    
    @FXML
    private void handleBrowseOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CMM Script");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CMM Scripts", "*.cmm")
        );
        chooser.setInitialFileName("flash_" + System.currentTimeMillis() + ".cmm");
        
        File file = chooser.showSaveDialog(getWindow());
        if (file != null) {
            txtOutputPath.setText(file.getAbsolutePath());
        }
    }
    
    // ========== HELPERS ==========
    
    private boolean validateForGeneration() {
        if (moduleRows.isEmpty()) {
            Alerts.warn("No modules loaded.");
            return false;
        }
        
        if (profileComboBox.getValue() == null) {
            Alerts.warn("Please select a target profile.");
            return false;
        }
        
        boolean hasInvalid = moduleRows.stream().anyMatch(r -> "INVALID".equals(r.getStatus()));
        if (hasInvalid) {
            if (!Alerts.confirm("Some modules have validation errors. Continue anyway?")) {
                return false;
            }
        }
        
        return true;
    }
    
    private void updateControlState() {
        boolean hasModules = !moduleRows.isEmpty();
        boolean isRunning = runningTask != null;
        boolean hasProfile = profileComboBox.getValue() != null;
        
        btnRemoveModule.setDisable(!hasModules || isRunning);
        btnValidate.setDisable(!hasModules || isRunning);
        btnGenerateCmm.setDisable(!hasModules || !hasProfile || isRunning);
        btnRunTrace32.setDisable(!hasModules || !hasProfile || isRunning);
        btnAbort.setDisable(!isRunning);
    }
    
    private void disableControls(boolean disable) {
        btnAddModule.setDisable(disable);
        btnRemoveModule.setDisable(disable);
        btnValidate.setDisable(disable);
        btnGenerateCmm.setDisable(disable);
        btnRunTrace32.setDisable(disable);
        btnAbort.setDisable(!disable);
        profileComboBox.setDisable(disable);
    }
    
    private void setStatus(String status) {
        Platform.runLater(() -> lblStatus.setText(status));
    }
    
    private void setProgress(double value) {
        Platform.runLater(() -> progressBar.setProgress(value));
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private long parseHexAddress(String text) {
        String cleaned = text.trim().toLowerCase();
        if (cleaned.startsWith("0x")) {
            cleaned = cleaned.substring(2);
        }
        return Long.parseUnsignedLong(cleaned, 16);
    }
    
    // ========== ROW CLASSES ==========
    
    /**
     * TableView row model for firmware modules.
     */
    public static class SoftwareModuleRow {
        private final UUID id;
        private final StringProperty name;
        private final Path path;
        private final long addressValue;
        private final StringProperty filePath;
        private final StringProperty address;
        private final StringProperty size;
        private final StringProperty checksum;
        private final StringProperty status;
        
        public SoftwareModuleRow(UUID id, String name, Path path, long address, long size, String checksum, String status) {
            this.id = id;
            this.path = path;
            this.addressValue = address;
            this.name = new SimpleStringProperty(name);
            this.filePath = new SimpleStringProperty(path.getFileName().toString());
            this.address = new SimpleStringProperty(String.format("0x%08X", address));
            this.size = new SimpleStringProperty(formatSize(size));
            this.checksum = new SimpleStringProperty(checksum);
            this.status = new SimpleStringProperty(status);
        }
        
        public UUID getId() { return id; }
        public String getName() { return name.get(); }
        public String getFilePath() { return filePath.get(); }
        public String getAddress() { return address.get(); }
        public long getAddressValue() { return addressValue; }
        public String getSize() { return size.get(); }
        public String getChecksum() { return checksum.get(); }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        
        public StringProperty nameProperty() { return name; }
        public StringProperty filePathProperty() { return filePath; }
        public StringProperty addressProperty() { return address; }
        public StringProperty sizeProperty() { return size; }
        public StringProperty checksumProperty() { return checksum; }
        public StringProperty statusProperty() { return status; }
        
        public SoftwareModule toSoftwareModule() {
            try {
                long fileSize = Files.size(path);
                String fullChecksum = com.aerospace.aitt.core.util.ChecksumUtil.sha256(path);
                return new SoftwareModule(id, getName(), path, addressValue, fileSize, fullChecksum);
            } catch (Exception e) {
                return new SoftwareModule(id, getName(), path, addressValue, 0, "");
            }
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * TableView row model for session history.
     */
    public static class FlashingSessionRow {
        private final UUID id;
        private final StringProperty timestamp;
        private final StringProperty targetProfile;
        private final IntegerProperty moduleCount;
        private final StringProperty status;
        
        public FlashingSessionRow(UUID id, String timestamp, String targetProfile, int moduleCount, String status) {
            this.id = id;
            this.timestamp = new SimpleStringProperty(timestamp);
            this.targetProfile = new SimpleStringProperty(targetProfile);
            this.moduleCount = new SimpleIntegerProperty(moduleCount);
            this.status = new SimpleStringProperty(status);
        }
        
        public UUID getId() { return id; }
        public String getTimestamp() { return timestamp.get(); }
        public String getTargetProfile() { return targetProfile.get(); }
        public int getModuleCount() { return moduleCount.get(); }
        public String getStatus() { return status.get(); }
        
        public StringProperty timestampProperty() { return timestamp; }
        public StringProperty targetProfileProperty() { return targetProfile; }
        public IntegerProperty moduleCountProperty() { return moduleCount; }
        public StringProperty statusProperty() { return status; }
    }
}
