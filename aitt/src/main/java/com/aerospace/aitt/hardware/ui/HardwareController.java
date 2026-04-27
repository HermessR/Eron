package com.aerospace.aitt.hardware.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.native_.HardwareInfo;
import com.aerospace.aitt.native_.HardwareService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * JavaFX Controller for Hardware Probe Management.
 * 
 * <p>Provides UI for:
 * <ul>
 *   <li>Detecting connected debug probes</li>
 *   <li>Connecting/disconnecting from probes</li>
 *   <li>Sending CMM commands</li>
 *   <li>Viewing command responses</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <p>This controller is loaded via FXML and integrates with the native
 * hardware bridge through {@link HardwareService}.</p>
 */
@SuppressWarnings("unused") // FXML handlers and fields are invoked via reflection
public class HardwareController extends BaseController {
    
    private static final AppLogger log = new AppLogger(HardwareController.class);
    
    // ==================== SERVICES ====================
    
    private volatile HardwareService hardwareService;
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    
    // ==================== FXML CONTROLS ====================
    
    // Loading Bar
    @FXML private StackPane loadingPane;
    @FXML private ProgressBar mainProgressBar;
    @FXML private Label lblLoadingStatus;
    @FXML private Button btnCancelOperation;
    
    // Probe Detection Panel
    @FXML private ListView<HardwareInfo> probeListView;
    @FXML private Button btnScanHardware;
    @FXML private Button btnScanPorts;
    @FXML private Label lblScanStatus;
    @FXML private ProgressIndicator scanProgress;
    
    // Connection Panel
    @FXML private ComboBox<String> chipsetComboBox;
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblProbeInfo;
    
    // Command Panel
    @FXML private TextField txtCommand;
    @FXML private Button btnSendCommand;
    @FXML private Button btnSystemUp;
    @FXML private Button btnSystemReset;
    @FXML private Button btnBreak;
    @FXML private Button btnGo;
    
    // Log Panel
    @FXML private TextArea logArea;
    @FXML private Button btnClearLog;
    
    // Status Bar
    @FXML private Label lblNativeVersion;
    @FXML private Label lblInterfaceType;
    @FXML private Label lblStatusBar;
    @FXML private Label lblConnectedProbe;
    @FXML private Label lblChipsetInfo;
    
    // ==================== DATA ====================
    
    private final ObservableList<HardwareInfo> probeList = FXCollections.observableArrayList();
    
    // ==================== INITIALIZATION ====================
    
    @Override
    public void initialize() {
        log.info("Initializing HardwareController");
        
        // Setup UI immediately (no native calls)
        setupProbeListView();
        setupChipsetComboBox();
        setupButtonHandlers();
        setupCommandField();
        
        // Initial state - disable until native is ready
        updateConnectionState(false);
        scanProgress.setVisible(false);
        setHardwareControlsDisabled(true);
        lblNativeVersion.setText("Loading native library...");
        
        // Initialize hardware service asynchronously to avoid blocking UI
        CompletableFuture.supplyAsync(() -> {
            try {
                return new HardwareService();
            } catch (Exception e) {
                log.error("Failed to initialize hardware service", e);
                return null;
            }
        }).thenAcceptAsync(service -> {
            if (service != null) {
                hardwareService = service;
                hardwareService.addConnectionListener(this::onConnectionEvent);
                hardwareService.addLogListener(this::appendLog);
                lblNativeVersion.setText("Native: v" + hardwareService.getNativeVersion());
                setHardwareControlsDisabled(false);
                log.info("HardwareController initialized");
            } else {
                lblNativeVersion.setText("Native library unavailable");
                Alerts.error("Native library not available. Hardware features disabled.");
                disableAllControls();
            }
        }, Platform::runLater);
    }
    
    private void setHardwareControlsDisabled(boolean disabled) {
        if (btnScanHardware != null) btnScanHardware.setDisable(disabled);
        if (btnScanPorts != null) btnScanPorts.setDisable(disabled);
        if (btnConnect != null) btnConnect.setDisable(disabled);
        if (btnSendCommand != null) btnSendCommand.setDisable(disabled);
    }
    
    private void setupProbeListView() {
        probeListView.setItems(probeList);
        probeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HardwareInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.displayName());
                    setStyle(item.isAvailable() 
                        ? "-fx-text-fill: #00ff88;" 
                        : "-fx-text-fill: #ff4444;");
                }
            }
        });
        
        probeListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateConnectButton()
        );
    }
    
    private void setupChipsetComboBox() {
        chipsetComboBox.setItems(FXCollections.observableArrayList(
            "S32K148",
            "MPC5777M"
        ));
        chipsetComboBox.getSelectionModel().selectFirst();
    }
    
    private void setupButtonHandlers() {
        btnScanHardware.setOnAction(e -> handleScanHardware());
        btnScanPorts.setOnAction(e -> handleScanPorts());
        btnConnect.setOnAction(e -> handleConnect());
        btnDisconnect.setOnAction(e -> handleDisconnect());
        btnSendCommand.setOnAction(e -> handleSendCommand());
        btnSystemUp.setOnAction(e -> sendQuickCommand("System.Up"));
        btnSystemReset.setOnAction(e -> sendQuickCommand("System.Reset"));
        btnBreak.setOnAction(e -> sendQuickCommand("Break"));
        btnGo.setOnAction(e -> sendQuickCommand("Go"));
        btnClearLog.setOnAction(e -> logArea.clear());
    }
    
    private void setupCommandField() {
        txtCommand.setOnAction(e -> handleSendCommand());
        txtCommand.setPromptText("Enter CMM command (e.g., System.Up, Data.Dump 0x08000000)");
    }
    
    // ==================== SCAN HANDLERS ====================
    
    @FXML
    private void handleScanHardware() {
        if (scanning.get()) return;
        
        scanning.set(true);
        scanProgress.setVisible(true);
        showLoading("Scanning for debug probes...");
        lblScanStatus.setText("Scanning for debug probes...");
        probeList.clear();
        
        hardwareService.detectHardwareAsync()
            .thenAccept(devices -> Platform.runLater(() -> {
                probeList.setAll(devices);
                lblScanStatus.setText("Found " + devices.size() + " device(s)");
                scanProgress.setVisible(false);
                hideLoading();
                scanning.set(false);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    lblScanStatus.setText("Scan failed: " + e.getMessage());
                    scanProgress.setVisible(false);
                    hideLoading();
                    scanning.set(false);
                });
                return null;
            });
    }
    
    @FXML
    private void handleScanPorts() {
        if (scanning.get()) return;
        
        scanning.set(true);
        scanProgress.setVisible(true);
        showLoading("Scanning for serial ports...");
        lblScanStatus.setText("Scanning for serial ports...");
        
        hardwareService.detectPortsAsync()
            .thenAccept(ports -> Platform.runLater(() -> {
                // Convert ports to HardwareInfo for display
                List<HardwareInfo> portInfos = ports.stream()
                    .map(p -> new HardwareInfo(
                        HardwareInfo.ProbeType.SERIAL, p, p, 
                        HardwareInfo.ProbeStatus.AVAILABLE))
                    .toList();
                probeList.setAll(portInfos);
                lblScanStatus.setText("Found " + ports.size() + " port(s)");
                scanProgress.setVisible(false);
                hideLoading();
                scanning.set(false);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    lblScanStatus.setText("Scan failed: " + e.getMessage());
                    scanProgress.setVisible(false);
                    hideLoading();
                    scanning.set(false);
                });
                return null;
            });
    }
    
    // ==================== CONNECTION HANDLERS ====================
    
    @FXML
    private void handleConnect() {
        HardwareInfo selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.warn("Please select a probe from the list.");
            return;
        }
        
        if (!selected.isAvailable()) {
            Alerts.warn("Selected probe is not available.");
            return;
        }
        
        String chipset = chipsetComboBox.getValue();
        if (chipset == null) {
            Alerts.warn("Please select a target chipset.");
            return;
        }
        
        btnConnect.setDisable(true);
        lblConnectionStatus.setText("Connecting...");
        showLoading("Connecting to " + selected.displayName() + "...");
        
        hardwareService.connectAsync(selected.id(), chipset)
            .thenAccept(success -> Platform.runLater(() -> {
                hideLoading();
                if (success) {
                    updateConnectionState(true);
                    lblProbeInfo.setText(selected.displayName());
                    if (lblInterfaceType != null) {
                        lblInterfaceType.setText(selected.type().name());
                    }
                } else {
                    updateConnectionState(false);
                    Alerts.error("Connection failed. Check probe and try again.");
                }
            }));
    }
    
    @FXML
    private void handleDisconnect() {
        showLoading("Disconnecting...");
        hardwareService.disconnect();
        updateConnectionState(false);
        lblProbeInfo.setText("-");
        hideLoading();
    }
    
    // ==================== COMMAND HANDLERS ====================
    
    @FXML
    private void handleSendCommand() {
        String command = txtCommand.getText().trim();
        if (command.isEmpty()) {
            return;
        }
        
        if (!hardwareService.isConnected()) {
            Alerts.warn("Not connected to any probe.");
            return;
        }
        
        txtCommand.clear();
        showLoading("Executing: " + command);
        sendCommand(command);
    }
    
    private void sendQuickCommand(String command) {
        if (!hardwareService.isConnected()) {
            Alerts.warn("Not connected to any probe.");
            return;
        }
        showLoading("Executing: " + command);
        sendCommand(command);
    }
    
    private void sendCommand(String command) {
        hardwareService.sendCommandAsync(command)
            .thenAccept(result -> Platform.runLater(() -> {
                hideLoading();
                if (!result.success()) {
                    appendLog("ERROR: " + result.errorMessage());
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    hideLoading();
                    appendLog("ERROR: " + e.getMessage());
                });
                return null;
            });
    }
    
    // ==================== EVENT HANDLERS ====================
    
    private void onConnectionEvent(HardwareService.ConnectionEvent event) {
        Platform.runLater(() -> {
            switch (event.state()) {
                case CONNECTING -> lblConnectionStatus.setText("Connecting...");
                case CONNECTED -> {
                    lblConnectionStatus.setText("Connected");
                    updateConnectionState(true);
                }
                case DISCONNECTED -> {
                    lblConnectionStatus.setText("Disconnected");
                    updateConnectionState(false);
                }
                case ERROR -> {
                    lblConnectionStatus.setText("Error");
                    appendLog("Connection error: " + event.message());
                    updateConnectionState(false);
                }
            }
        });
    }
    
    // ==================== UI HELPERS ====================
    
    private void updateConnectionState(boolean connected) {
        btnConnect.setDisable(connected);
        btnDisconnect.setDisable(!connected);
        btnSendCommand.setDisable(!connected);
        btnSystemUp.setDisable(!connected);
        btnSystemReset.setDisable(!connected);
        btnBreak.setDisable(!connected);
        btnGo.setDisable(!connected);
        txtCommand.setDisable(!connected);
        
        if (connected) {
            lblConnectionStatus.setText("Status: Connected");
            lblConnectionStatus.setStyle("-fx-text-fill: #00ff88;");
            updateStatusBar("Connected", chipsetComboBox.getValue());
        } else {
            lblConnectionStatus.setText("Status: Disconnected");
            lblConnectionStatus.setStyle("-fx-text-fill: #888888;");
            lblProbeInfo.setText("-");
            if (lblInterfaceType != null) lblInterfaceType.setText("-");
            updateStatusBar("Disconnected", null);
        }
    }
    
    private void updateStatusBar(String status, String chipset) {
        if (lblStatusBar != null) {
            lblStatusBar.setText(status);
        }
        if (lblConnectedProbe != null) {
            HardwareInfo selected = probeListView.getSelectionModel().getSelectedItem();
            boolean connected = hardwareService != null && hardwareService.isConnected();
            lblConnectedProbe.setText(selected != null && connected 
                ? selected.displayName() : "No probe connected");
        }
        if (lblChipsetInfo != null) {
            lblChipsetInfo.setText(chipset != null ? "Chipset: " + chipset : "Chipset: None");
        }
    }
    
    private void updateConnectButton() {
        HardwareInfo selected = probeListView.getSelectionModel().getSelectedItem();
        boolean connected = hardwareService != null && hardwareService.isConnected();
        btnConnect.setDisable(selected == null || !selected.isAvailable() || connected);
    }
    
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private void disableAllControls() {
        btnScanHardware.setDisable(true);
        btnScanPorts.setDisable(true);
        btnConnect.setDisable(true);
        btnDisconnect.setDisable(true);
        btnSendCommand.setDisable(true);
        txtCommand.setDisable(true);
        lblConnectionStatus.setText("Native library unavailable");
        lblConnectionStatus.setStyle("-fx-text-fill: #ff4444;");
    }
    
    // ==================== LOADING BAR ====================
    
    /**
     * Shows the loading bar with indeterminate progress.
     */
    private void showLoading(String statusText) {
        showLoading(statusText, -1);
    }
    
    /**
     * Shows the loading bar with specified progress (0.0 to 1.0, or -1 for indeterminate).
     */
    private void showLoading(String statusText, double progress) {
        Platform.runLater(() -> {
            operationInProgress.set(true);
            loadingPane.setManaged(true);
            loadingPane.setVisible(true);
            mainProgressBar.setProgress(progress);
            lblLoadingStatus.setText(statusText);
        });
    }
    
    /**
     * Updates the loading bar progress and status text.
     */
    private void updateLoading(String statusText, double progress) {
        Platform.runLater(() -> {
            mainProgressBar.setProgress(progress);
            lblLoadingStatus.setText(statusText);
        });
    }
    
    /**
     * Hides the loading bar.
     */
    private void hideLoading() {
        Platform.runLater(() -> {
            operationInProgress.set(false);
            loadingPane.setManaged(false);
            loadingPane.setVisible(false);
        });
    }
    
    /**
     * Handler for cancel button on loading bar.
     */
    @FXML
    private void handleCancelOperation() {
        if (operationInProgress.get()) {
            log.info("Operation cancelled by user");
            hideLoading();
            appendLog("[CANCELLED] Operation cancelled by user");
        }
    }
    
    // ==================== NAVIGATION ====================
    
    @FXML
    private void handleBack() {
        if (hardwareService != null && hardwareService.isConnected()) {
            if (!Alerts.confirm("Disconnect from probe and return to dashboard?")) {
                return;
            }
            hardwareService.disconnect();
        }
        navigateTo("dashboard");
    }
    
    // ==================== CLEANUP ====================
    
    @Override
    public void onClose() {
        if (hardwareService != null) {
            hardwareService.close();
        }
    }
}
