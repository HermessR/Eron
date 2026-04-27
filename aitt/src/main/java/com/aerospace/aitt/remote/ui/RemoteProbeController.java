package com.aerospace.aitt.remote.ui;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.core.ui.StageManager;
import com.aerospace.aitt.remote.model.NetworkProbe;
import com.aerospace.aitt.remote.model.NetworkProbe.ConnectionStatus;
import com.aerospace.aitt.remote.model.NetworkProbe.RemoteType;
import com.aerospace.aitt.remote.service.NetworkProbeService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Controller for Remote Probe Management UI.
 * 
 * <p>Provides functionality to:
 * <ul>
 *   <li>Discover network-attached probes</li>
 *   <li>Add probes manually</li>
 *   <li>Connect/disconnect from probes</li>
 *   <li>Test probe connections</li>
 *   <li>Launch remote flashing</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class RemoteProbeController extends BaseController {
    
    private static final AppLogger log = new AppLogger(RemoteProbeController.class);
    
    // ==================== SERVICES ====================
    
    private volatile NetworkProbeService probeService;
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    
    // ==================== FXML CONTROLS ====================
    
    // Loading Bar
    @FXML private StackPane loadingPane;
    @FXML private ProgressBar mainProgressBar;
    @FXML private Label lblLoadingStatus;
    @FXML private Button btnCancelOperation;
    
    // Probe List Panel
    @FXML private ListView<NetworkProbe> probeListView;
    @FXML private Button btnDiscover;
    @FXML private Button btnAddManual;
    @FXML private Button btnRemove;
    @FXML private Label lblScanStatus;
    @FXML private ProgressIndicator scanProgress;
    
    // Connection Panel
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;
    @FXML private Button btnTestConnection;
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblProbeDetails;
    
    // Actions Panel
    @FXML private Button btnFlashRemote;
    @FXML private Button btnSendCommand;
    @FXML private TextField txtCommand;
    
    // Log Panel
    @FXML private TextArea logArea;
    @FXML private Button btnClearLog;
    
    // Status Bar
    @FXML private Label lblStatusBar;
    @FXML private Label lblConnectedCount;
    @FXML private Label lblDiscoveredCount;
    
    // ==================== DATA ====================
    
    private final ObservableList<NetworkProbe> probeList = FXCollections.observableArrayList();
    
    // ==================== INITIALIZATION ====================
    
    @Override
    public void initialize() {
        log.info("Initializing RemoteProbeController");
        
        setupProbeListView();
        setupButtonHandlers();
        
        // Initial state
        scanProgress.setVisible(false);
        setControlsDisabled(true);
        
        // Initialize service asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return new NetworkProbeService();
            } catch (Exception e) {
                log.error("Failed to initialize network probe service", e);
                return null;
            }
        }).thenAcceptAsync(service -> {
            if (service != null) {
                probeService = service;
                probeService.addDiscoveryListener(this::onProbeDiscovered);
                probeService.addLogListener(this::appendLog);
                probeService.startKeepAlive();
                setControlsDisabled(false);
                updateStatusBar();
                log.info("RemoteProbeController initialized");
            } else {
                Alerts.error("Failed to initialize network services");
                disableAllControls();
            }
        }, Platform::runLater);
    }
    
    private void setupProbeListView() {
        probeListView.setItems(probeList);
        probeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NetworkProbe probe, boolean empty) {
                super.updateItem(probe, empty);
                if (empty || probe == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Create custom cell
                    VBox container = new VBox(3);
                    container.setPadding(new Insets(5));
                    
                    Label nameLabel = new Label(probe.name());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                    
                    Label addressLabel = new Label(probe.address() + " • " + probe.type().displayName());
                    addressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
                    
                    Label statusLabel = new Label(probe.status().displayName());
                    statusLabel.setStyle(getStatusStyle(probe.status()));
                    
                    container.getChildren().addAll(nameLabel, addressLabel, statusLabel);
                    
                    setGraphic(container);
                    setText(null);
                    
                    // Highlight connected probes
                    if (probe.status() == ConnectionStatus.CONNECTED) {
                        setStyle("-fx-background-color: rgba(0, 200, 100, 0.1);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        // Selection listener
        probeListView.getSelectionModel().selectedItemProperty().addListener((obs, old, probe) -> {
            onProbeSelected(probe);
        });
    }
    
    private String getStatusStyle(ConnectionStatus status) {
        return switch (status) {
            case CONNECTED -> "-fx-font-size: 11px; -fx-text-fill: #00c853;";
            case DISCOVERED -> "-fx-font-size: 11px; -fx-text-fill: #2196f3;";
            case CONNECTING -> "-fx-font-size: 11px; -fx-text-fill: #ff9800;";
            case BUSY -> "-fx-font-size: 11px; -fx-text-fill: #ff9800;";
            case DISCONNECTED -> "-fx-font-size: 11px; -fx-text-fill: #9e9e9e;";
            case UNREACHABLE, ERROR -> "-fx-font-size: 11px; -fx-text-fill: #f44336;";
            default -> "-fx-font-size: 11px; -fx-text-fill: #888;";
        };
    }
    
    private void setupButtonHandlers() {
        // Probes will be selected from list, enable/disable buttons accordingly
    }
    
    private void setControlsDisabled(boolean disabled) {
        if (btnDiscover != null) btnDiscover.setDisable(disabled);
        if (btnAddManual != null) btnAddManual.setDisable(disabled);
    }
    
    private void disableAllControls() {
        setControlsDisabled(true);
        if (btnConnect != null) btnConnect.setDisable(true);
        if (btnDisconnect != null) btnDisconnect.setDisable(true);
        if (btnFlashRemote != null) btnFlashRemote.setDisable(true);
    }
    
    // ==================== EVENT HANDLERS ====================
    
    private void onProbeDiscovered(NetworkProbe probe) {
        Platform.runLater(() -> {
            // Update or add probe
            int existingIndex = -1;
            for (int i = 0; i < probeList.size(); i++) {
                if (probeList.get(i).id().equals(probe.id())) {
                    existingIndex = i;
                    break;
                }
            }
            
            if (existingIndex >= 0) {
                probeList.set(existingIndex, probe);
            } else {
                probeList.add(probe);
            }
            
            updateStatusBar();
        });
    }
    
    private void onProbeSelected(NetworkProbe probe) {
        boolean hasSelection = probe != null;
        
        if (btnRemove != null) btnRemove.setDisable(!hasSelection);
        if (btnTestConnection != null) btnTestConnection.setDisable(!hasSelection);
        
        if (hasSelection) {
            boolean isConnected = probe.status() == ConnectionStatus.CONNECTED;
            
            if (btnConnect != null) btnConnect.setDisable(isConnected);
            if (btnDisconnect != null) btnDisconnect.setDisable(!isConnected);
            if (btnFlashRemote != null) btnFlashRemote.setDisable(!isConnected);
            if (btnSendCommand != null) btnSendCommand.setDisable(!isConnected);
            
            updateProbeDetails(probe);
        } else {
            if (btnConnect != null) btnConnect.setDisable(true);
            if (btnDisconnect != null) btnDisconnect.setDisable(true);
            if (btnFlashRemote != null) btnFlashRemote.setDisable(true);
            if (btnSendCommand != null) btnSendCommand.setDisable(true);
            
            if (lblProbeDetails != null) lblProbeDetails.setText("No probe selected");
        }
    }
    
    private void updateProbeDetails(NetworkProbe probe) {
        if (lblProbeDetails != null) {
            String details = String.format(
                "Type: %s%nAddress: %s%nStatus: %s%nLast seen: %s",
                probe.type().displayName(),
                probe.address(),
                probe.status().displayName(),
                probe.lastSeen()
            );
            lblProbeDetails.setText(details);
        }
    }
    
    // ==================== FXML ACTIONS ====================
    
    @FXML
    private void handleBack() {
        StageManager.getInstance().switchScene("/fxml/dashboard.fxml");
    }
    
    @FXML
    private void handleDiscover() {
        if (probeService == null || scanning.get()) return;
        
        scanning.set(true);
        scanProgress.setVisible(true);
        lblScanStatus.setText("Scanning network...");
        btnDiscover.setDisable(true);
        
        appendLog("Starting network discovery...");
        
        probeService.discoverAll()
            .thenAcceptAsync(probes -> {
                scanning.set(false);
                scanProgress.setVisible(false);
                lblScanStatus.setText("Found " + probes.size() + " probe(s)");
                btnDiscover.setDisable(false);
                
                appendLog("Discovery complete: " + probes.size() + " probes found");
            }, Platform::runLater)
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    scanning.set(false);
                    scanProgress.setVisible(false);
                    lblScanStatus.setText("Discovery failed");
                    btnDiscover.setDisable(false);
                    
                    appendLog("ERROR: " + e.getMessage());
                });
                return null;
            });
    }
    
    @FXML
    private void handleAddManual() {
        // Create dialog for manual probe entry
        Dialog<NetworkProbe> dialog = new Dialog<>();
        dialog.setTitle("Add Remote Probe");
        dialog.setHeaderText("Enter probe connection details");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Probe Name");
        TextField hostField = new TextField();
        hostField.setPromptText("IP Address or Hostname");
        TextField portField = new TextField("20000");
        portField.setPromptText("Port");
        ComboBox<RemoteType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(RemoteType.values());
        typeCombo.setValue(RemoteType.T32_REMOTE_API);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Host:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText().trim();
                    String host = hostField.getText().trim();
                    int port = Integer.parseInt(portField.getText().trim());
                    RemoteType type = typeCombo.getValue();
                    
                    if (name.isEmpty()) name = type.displayName() + " @ " + host;
                    
                    return NetworkProbe.manual(name, host, port, type);
                } catch (NumberFormatException e) {
                    Alerts.error("Invalid port number");
                    return null;
                }
            }
            return null;
        });
        
        Optional<NetworkProbe> result = dialog.showAndWait();
        result.ifPresent(probe -> {
            appendLog("Adding probe: " + probe.address());
            
            probeService.addManualProbe(probe.name(), probe.host(), probe.port(), probe.type())
                .thenAcceptAsync(added -> {
                    appendLog("Probe added: " + added.displayString());
                }, Platform::runLater)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        appendLog("ERROR: Failed to add probe - " + e.getMessage());
                        Alerts.error("Failed to add probe: " + e.getCause().getMessage());
                    });
                    return null;
                });
        });
    }
    
    @FXML
    private void handleRemove() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        if (selected.status() == ConnectionStatus.CONNECTED) {
            probeService.disconnect(selected);
        }
        probeList.remove(selected);
        appendLog("Removed probe: " + selected.name());
    }
    
    @FXML
    private void handleConnect() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null || probeService == null) return;
        
        appendLog("Connecting to " + selected.address() + "...");
        btnConnect.setDisable(true);
        
        probeService.connect(selected)
            .thenAcceptAsync(success -> {
                if (success) {
                    appendLog("Connected to " + selected.name());
                    lblConnectionStatus.setText("Connected");
                } else {
                    appendLog("Failed to connect");
                    lblConnectionStatus.setText("Connection failed");
                }
                onProbeSelected(probeListView.getSelectionModel().getSelectedItem());
            }, Platform::runLater);
    }
    
    @FXML
    private void handleDisconnect() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null || probeService == null) return;
        
        probeService.disconnect(selected);
        appendLog("Disconnected from " + selected.name());
        lblConnectionStatus.setText("Disconnected");
        onProbeSelected(probeListView.getSelectionModel().getSelectedItem());
    }
    
    @FXML
    private void handleTestConnection() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null || probeService == null) return;
        
        appendLog("Testing connection to " + selected.address() + "...");
        
        probeService.addManualProbe(selected.name(), selected.host(), selected.port(), selected.type())
            .thenAcceptAsync(probe -> {
                appendLog("Connection test successful: " + probe.address());
                Alerts.info("Connection successful!");
            }, Platform::runLater)
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    appendLog("Connection test failed: " + e.getCause().getMessage());
                    Alerts.error("Connection failed: " + e.getCause().getMessage());
                });
                return null;
            });
    }
    
    @FXML
    private void handleFlashRemote() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.status() != ConnectionStatus.CONNECTED) {
            Alerts.warning("Connection Required", "Please connect to a probe first");
            return;
        }
        
        // Navigate to flashing module
        StageManager.getInstance().switchScene("/fxml/flashing.fxml");
    }
    
    @FXML
    private void handleSendCommand() {
        NetworkProbe selected = probeListView.getSelectionModel().getSelectedItem();
        if (selected == null || probeService == null) return;
        
        String command = txtCommand.getText().trim();
        if (command.isEmpty()) return;
        
        appendLog("> " + command);
        txtCommand.clear();
        
        probeService.sendCommand(selected, command)
            .thenAcceptAsync(response -> {
                appendLog("< " + response);
            }, Platform::runLater)
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    appendLog("ERROR: " + e.getCause().getMessage());
                });
                return null;
            });
    }
    
    @FXML
    private void handleClearLog() {
        if (logArea != null) {
            logArea.clear();
        }
    }
    
    @FXML
    private void handleCancelOperation() {
        // Cancel current operation
        operationInProgress.set(false);
        if (loadingPane != null) {
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
        }
    }
    
    // ==================== UTILITIES ====================
    
    private void appendLog(String message) {
        if (logArea != null) {
            Platform.runLater(() -> {
                logArea.appendText(message + "\n");
            });
        }
    }
    
    private void updateStatusBar() {
        if (probeService == null) return;
        
        long connected = probeList.stream()
            .filter(p -> p.status() == ConnectionStatus.CONNECTED)
            .count();
        
        if (lblConnectedCount != null) {
            lblConnectedCount.setText("Connected: " + connected);
        }
        if (lblDiscoveredCount != null) {
            lblDiscoveredCount.setText("Discovered: " + probeList.size());
        }
    }
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void onShow() {
        if (probeService != null) {
            probeService.startKeepAlive();
        }
        log.info("Remote Probe Manager displayed");
    }
    
    @Override
    public void onHide() {
        // Close the service when leaving
        if (probeService != null) {
            probeService.close();
        }
        log.info("RemoteProbeController cleanup");
    }
}
