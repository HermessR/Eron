package com.aerospace.aitt.native_;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.aerospace.aitt.core.log.AppLogger;

/**
 * High-level service for hardware operations.
 * 
 * <p>Wraps {@link HardwareBridge} with:
 * <ul>
 *   <li>Asynchronous operation support</li>
 *   <li>Connection state management</li>
 *   <li>Event callbacks for UI updates</li>
 *   <li>Automatic reconnection</li>
 * </ul>
 */
public class HardwareService implements AutoCloseable {
    
    private static final AppLogger log = new AppLogger(HardwareService.class);
    
    private final HardwareBridge bridge;
    private final ExecutorService executor;
    private final List<Consumer<ConnectionEvent>> connectionListeners;
    private final List<Consumer<String>> logListeners;
    
    @SuppressWarnings("unused") // Reserved for future auto-reconnect feature
    private volatile String selectedChipset = null;
    @SuppressWarnings("unused") // Reserved for future auto-reconnect feature
    private volatile boolean autoReconnect = false;
    
    /**
     * Connection state events.
     */
    public enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
    
    /**
     * Connection event record.
     */
    public record ConnectionEvent(
        ConnectionState state,
        String probeId,
        String message
    ) {}
    
    /**
     * Creates a new HardwareService.
     */
    public HardwareService() {
        if (!HardwareBridge.isNativeAvailable()) {
            throw new IllegalStateException("Native library not available");
        }
        
        this.bridge = HardwareBridge.getInstance();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hardware-service");
            t.setDaemon(true);
            return t;
        });
        this.connectionListeners = new CopyOnWriteArrayList<>();
        this.logListeners = new CopyOnWriteArrayList<>();
        
        log.info("HardwareService initialized");
    }
    
    // ==================== LISTENERS ====================
    
    /**
     * Adds a connection state listener.
     */
    public void addConnectionListener(Consumer<ConnectionEvent> listener) {
        connectionListeners.add(listener);
    }
    
    /**
     * Removes a connection state listener.
     */
    public void removeConnectionListener(Consumer<ConnectionEvent> listener) {
        connectionListeners.remove(listener);
    }
    
    /**
     * Adds a log listener for hardware messages.
     */
    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }
    
    private void fireConnectionEvent(ConnectionState state, String probeId, String message) {
        ConnectionEvent event = new ConnectionEvent(state, probeId, message);
        connectionListeners.forEach(l -> {
            try {
                l.accept(event);
            } catch (Exception e) {
                log.error("Connection listener error", e);
            }
        });
    }
    
    private void fireLog(String message) {
        logListeners.forEach(l -> {
            try {
                l.accept(message);
            } catch (Exception ignored) {}
        });
    }
    
    // ==================== DETECTION ====================
    
    /**
     * Detects connected hardware asynchronously.
     */
    public CompletableFuture<List<HardwareInfo>> detectHardwareAsync() {
        return CompletableFuture.supplyAsync(() -> {
            fireLog("Scanning for hardware probes...");
            List<HardwareInfo> devices = bridge.detectConnectedHardware();
            fireLog("Found " + devices.size() + " device(s)");
            return devices;
        }, executor);
    }
    
    /**
     * Detects connected hardware synchronously.
     */
    public List<HardwareInfo> detectHardware() {
        return bridge.detectConnectedHardware();
    }
    
    /**
     * Detects available COM ports asynchronously.
     */
    public CompletableFuture<List<String>> detectPortsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            fireLog("Scanning for serial ports...");
            List<String> ports = bridge.detectAvailablePorts();
            fireLog("Found " + ports.size() + " port(s)");
            return ports;
        }, executor);
    }
    
    /**
     * Detects available COM ports synchronously.
     */
    public List<String> detectPorts() {
        return bridge.detectAvailablePorts();
    }
    
    // ==================== CONNECTION ====================
    
    /**
     * Connects to a probe asynchronously.
     */
    public CompletableFuture<Boolean> connectAsync(String probeId, String chipset) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                fireConnectionEvent(ConnectionState.CONNECTING, probeId, "Connecting...");
                fireLog("Connecting to " + probeId + " for " + chipset);
                
                // Configure chipset first
                ChipsetConfig config = switch (chipset.toUpperCase()) {
                    case "S32K148" -> ChipsetConfig.forS32K148();
                    case "MPC5777M" -> ChipsetConfig.forMPC5777M();
                    default -> ChipsetConfig.custom(chipset);
                };
                
                bridge.configureChipset(chipset, config);
                selectedChipset = chipset;
                
                // Connect to probe
                boolean success = bridge.connectProbe(probeId);
                
                if (success) {
                    fireConnectionEvent(ConnectionState.CONNECTED, probeId, "Connected to " + probeId);
                    fireLog("Connected successfully");
                } else {
                    fireConnectionEvent(ConnectionState.ERROR, probeId, "Connection failed: " + bridge.getLastError());
                    fireLog("Connection failed: " + bridge.getLastError());
                }
                
                return success;
                
            } catch (Exception e) {
                fireConnectionEvent(ConnectionState.ERROR, probeId, "Error: " + e.getMessage());
                fireLog("Error: " + e.getMessage());
                log.error("Connection error", e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Connects synchronously.
     */
    public boolean connect(String probeId, String chipset) {
        try {
            return connectAsync(probeId, chipset).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Connect failed", e);
            return false;
        }
    }
    
    /**
     * Disconnects from current probe.
     */
    public void disconnect() {
        if (bridge.isConnected()) {
            bridge.disconnect();
            fireConnectionEvent(ConnectionState.DISCONNECTED, null, "Disconnected");
            fireLog("Disconnected");
        }
    }
    
    /**
     * Checks if connected.
     */
    public boolean isConnected() {
        return bridge.isConnected();
    }
    
    /**
     * Gets current probe ID.
     */
    public String getCurrentProbeId() {
        return bridge.getCurrentProbeId();
    }
    
    // ==================== COMMANDS ====================
    
    /**
     * Sends a CMM command asynchronously.
     */
    public CompletableFuture<CommandResult> sendCommandAsync(String command) {
        return CompletableFuture.supplyAsync(() -> {
            fireLog(">>> " + command);
            CommandResult result = bridge.sendCommand(command);
            if (result.success()) {
                fireLog("<<< " + result.response());
            } else {
                fireLog("<<< ERROR: " + result.errorMessage());
            }
            return result;
        }, executor);
    }
    
    /**
     * Sends a CMM command synchronously.
     */
    public CommandResult sendCommand(String command) {
        return bridge.sendCommand(command);
    }
    
    /**
     * Sends a CMM command and waits for result.
     */
    public CommandResult sendCommandAndWait(String command, int timeoutMs) {
        try {
            return sendCommandAsync(command).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return CommandResult.error(-6, "Command timed out");
        } catch (Exception e) {
            return CommandResult.error(-99, e.getMessage());
        }
    }
    
    // ==================== MEMORY ====================
    
    /**
     * Reads memory asynchronously.
     */
    public CompletableFuture<byte[]> readMemoryAsync(long address, int length) {
        return CompletableFuture.supplyAsync(() -> {
            fireLog(String.format("Reading %d bytes from 0x%08X", length, address));
            byte[] data = bridge.readMemory(address, length);
            fireLog(String.format("Read %d bytes", data.length));
            return data;
        }, executor);
    }
    
    /**
     * Writes memory asynchronously.
     */
    public CompletableFuture<Void> writeMemoryAsync(long address, byte[] data) {
        return CompletableFuture.runAsync(() -> {
            fireLog(String.format("Writing %d bytes to 0x%08X", data.length, address));
            bridge.writeMemory(address, data);
            fireLog("Write complete");
        }, executor);
    }
    
    // ==================== CHIPSET COMMANDS ====================
    
    /**
     * Initializes the target system.
     */
    public CompletableFuture<CommandResult> systemUpAsync() {
        return sendCommandAsync("System.Up");
    }
    
    /**
     * Resets the target system.
     */
    public CompletableFuture<CommandResult> systemResetAsync() {
        return sendCommandAsync("System.Reset");
    }
    
    /**
     * Halts the target CPU.
     */
    public CompletableFuture<CommandResult> breakAsync() {
        return sendCommandAsync("Break");
    }
    
    /**
     * Runs the target CPU.
     */
    public CompletableFuture<CommandResult> goAsync() {
        return sendCommandAsync("Go");
    }
    
    /**
     * Gets CPU status.
     */
    public CompletableFuture<CommandResult> getCpuStatusAsync() {
        return sendCommandAsync("System.CPU");
    }
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void close() {
        disconnect();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        connectionListeners.clear();
        logListeners.clear();
        log.info("HardwareService closed");
    }
    
    /**
     * Gets the native library version.
     */
    public String getNativeVersion() {
        return bridge.getNativeVersion();
    }
}
