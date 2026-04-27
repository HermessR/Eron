package com.aerospace.aitt.native_;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.aerospace.aitt.core.log.AppLogger;

/**
 * JNI bridge for low-level hardware communication.
 * 
 * <p>This class provides native methods for:
 * <ul>
 *   <li>Hardware probe detection (USB, Serial)</li>
 *   <li>Debug command execution (CMM/TRACE32)</li>
 *   <li>Chipset-specific communication protocols</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * HardwareBridge bridge = HardwareBridge.getInstance();
 * 
 * // Detect connected hardware
 * List<HardwareInfo> devices = bridge.detectConnectedHardware();
 * 
 * // Connect to a probe
 * boolean connected = bridge.connectProbe("USB:0001");
 * 
 * // Send command
 * CommandResult result = bridge.sendCommand("System.Up");
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>Native methods are thread-safe. The underlying C++ implementation
 * uses mutex locks for concurrent access protection.</p>
 * 
 * @since 1.0
 */
public final class HardwareBridge implements AutoCloseable {
    
    private static final AppLogger log = new AppLogger(HardwareBridge.class);
    
    /** Native library name (without extension) */
    private static final String NATIVE_LIB_NAME = "aitt_native";
    
    /** Singleton instance */
    private static volatile HardwareBridge instance;
    
    /** Lock for thread-safe initialization */
    private static final Object LOCK = new Object();
    
    /** Whether native library is loaded */
    private static volatile boolean libraryLoaded = false;
    
    /** Whether bridge is connected to a probe */
    private volatile boolean connected = false;
    
    /** Current probe identifier */
    private volatile String currentProbeId = null;
    
    // ==================== NATIVE METHOD DECLARATIONS ====================
    
    /**
     * Initializes the native hardware subsystem.
     * Must be called before any other native methods.
     * 
     * @return true if initialization succeeded
     */
    private native boolean nativeInit();
    
    /**
     * Shuts down the native hardware subsystem.
     * Releases all resources and closes connections.
     */
    private native void nativeShutdown();
    
    /**
     * Detects all connected debug probes.
     * 
     * @return Array of probe info strings in format "TYPE:ID:NAME"
     */
    private native String[] nativeDetectHardware();
    
    /**
     * Detects available serial/COM ports.
     * 
     * @return Array of port names (e.g., "COM3", "COM4")
     */
    private native String[] nativeDetectPorts();
    
    /**
     * Connects to a specific debug probe.
     * 
     * @param probeId Probe identifier from detection
     * @return 0 on success, error code otherwise
     */
    private native int nativeConnectProbe(String probeId);
    
    /**
     * Disconnects from the current probe.
     * 
     * @return 0 on success, error code otherwise
     */
    private native int nativeDisconnectProbe();
    
    /**
     * Sends a CMM command to the connected chipset.
     * 
     * @param command CMM command string
     * @return Response string or error message
     */
    @SuppressWarnings("unused") // Reserved for future direct command execution
    private native String nativeSendCommand(String command);
    
    /**
     * Sends a CMM command with timeout.
     * 
     * @param command CMM command string
     * @param timeoutMs Timeout in milliseconds
     * @return Response string or error message
     */
    private native String nativeSendCommandWithTimeout(String command, int timeoutMs);
    
    /**
     * Reads memory from the target chipset.
     * 
     * @param address Start address
     * @param length Number of bytes to read
     * @return Byte array of memory contents, or null on error
     */
    private native byte[] nativeReadMemory(long address, int length);
    
    /**
     * Writes memory to the target chipset.
     * 
     * @param address Start address
     * @param data Data to write
     * @return Number of bytes written, or negative error code
     */
    private native int nativeWriteMemory(long address, byte[] data);
    
    /**
     * Gets the last error message from native code.
     * 
     * @return Error message string
     */
    private native String nativeGetLastError();
    
    /**
     * Gets native library version.
     * 
     * @return Version string
     */
    private native String nativeGetVersion();
    
    /**
     * Configures chipset-specific settings.
     * 
     * @param chipsetId Chipset identifier (e.g., "S32K148", "MPC5777M")
     * @param configJson JSON configuration string
     * @return 0 on success, error code otherwise
     */
    private native int nativeConfigureChipset(String chipsetId, String configJson);
    
    // ==================== LIBRARY LOADING ====================
    
    static {
        loadNativeLibrary();
    }
    
    /**
     * Loads the native library from resources or system path.
     */
    private static void loadNativeLibrary() {
        if (libraryLoaded) return;
        
        synchronized (LOCK) {
            if (libraryLoaded) return;
            
            try {
                // First try loading from java.library.path
                System.loadLibrary(NATIVE_LIB_NAME);
                libraryLoaded = true;
                log.info("Native library loaded from system path: %s", NATIVE_LIB_NAME);
            } catch (UnsatisfiedLinkError e1) {
                // Try extracting from JAR resources
                try {
                    loadFromResources();
                    libraryLoaded = true;
                } catch (Exception e2) {
                    log.error("Failed to load native library", e2);
                    throw new RuntimeException(
                        "Cannot load native library '" + NATIVE_LIB_NAME + "'. " +
                        "Ensure aitt_native.dll is in java.library.path or in resources/native/",
                        e2
                    );
                }
            }
        }
    }
    
    /**
     * Extracts and loads native library from JAR resources.
     */
    private static void loadFromResources() throws IOException {
        String libFileName = NATIVE_LIB_NAME + ".dll";
        String resourcePath = "/native/windows-x64/" + libFileName;
        
        try (InputStream is = HardwareBridge.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Native library not found in resources: " + resourcePath);
            }
            
            // Extract to temp directory
            Path tempDir = Files.createTempDirectory("aitt_native");
            Path tempLib = tempDir.resolve(libFileName);
            
            Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
            tempLib.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            
            System.load(tempLib.toAbsolutePath().toString());
            log.info("Native library extracted and loaded from: %s", tempLib);
        }
    }
    
    // ==================== SINGLETON & LIFECYCLE ====================
    
    /**
     * Private constructor - use getInstance().
     */
    private HardwareBridge() {
        if (!libraryLoaded) {
            throw new IllegalStateException("Native library not loaded");
        }
        
        if (!nativeInit()) {
            throw new RuntimeException("Failed to initialize native hardware subsystem: " + 
                                       nativeGetLastError());
        }
        
        log.info("HardwareBridge initialized - Native version: %s", nativeGetVersion());
    }
    
    /**
     * Gets the singleton instance of HardwareBridge.
     * 
     * @return The HardwareBridge instance
     * @throws RuntimeException if native library cannot be loaded
     */
    public static HardwareBridge getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new HardwareBridge();
                }
            }
        }
        return instance;
    }
    
    /**
     * Checks if the native library is available.
     * 
     * @return true if native library is loaded
     */
    public static boolean isNativeAvailable() {
        return libraryLoaded;
    }
    
    @Override
    public void close() {
        if (connected) {
            disconnect();
        }
        nativeShutdown();
        log.info("HardwareBridge shutdown complete");
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Detects all connected debug hardware probes.
     * 
     * @return List of detected hardware info
     */
    public List<HardwareInfo> detectConnectedHardware() {
        checkLibraryLoaded();
        
        String[] rawResults = nativeDetectHardware();
        if (rawResults == null || rawResults.length == 0) {
            log.info("No hardware probes detected");
            return List.of();
        }
        
        return java.util.Arrays.stream(rawResults)
            .map(HardwareInfo::parse)
            .filter(h -> h != null)
            .toList();
    }
    
    /**
     * Detects available serial/COM ports.
     * 
     * @return List of port names
     */
    public List<String> detectAvailablePorts() {
        checkLibraryLoaded();
        
        String[] ports = nativeDetectPorts();
        if (ports == null) {
            return List.of();
        }
        return List.of(ports);
    }
    
    /**
     * Connects to a debug probe.
     * 
     * @param probeId Probe identifier (from HardwareInfo)
     * @return true if connection succeeded
     */
    public boolean connectProbe(String probeId) {
        checkLibraryLoaded();
        
        if (connected) {
            log.warn("Already connected to %s, disconnecting first", currentProbeId);
            disconnect();
        }
        
        int result = nativeConnectProbe(probeId);
        if (result == 0) {
            connected = true;
            currentProbeId = probeId;
            log.info("Connected to probe: %s", probeId);
            return true;
        } else {
            log.error("Failed to connect to probe %s: %s (code %d)", 
                      probeId, nativeGetLastError(), result);
            return false;
        }
    }
    
    /**
     * Disconnects from the current probe.
     */
    public void disconnect() {
        if (!connected) return;
        
        int result = nativeDisconnectProbe();
        if (result != 0) {
            log.warn("Disconnect returned code %d: %s", result, nativeGetLastError());
        }
        
        connected = false;
        currentProbeId = null;
        log.info("Disconnected from probe");
    }
    
    /**
     * Checks if connected to a probe.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Gets the current probe identifier.
     * 
     * @return Probe ID or null if not connected
     */
    public String getCurrentProbeId() {
        return currentProbeId;
    }
    
    /**
     * Sends a CMM command to the chipset.
     * 
     * @param command CMM command string
     * @return Command result
     * @throws IllegalStateException if not connected
     */
    public CommandResult sendCommand(String command) {
        return sendCommand(command, 5000); // Default 5 second timeout
    }
    
    /**
     * Sends a CMM command with specified timeout.
     * 
     * @param command CMM command string
     * @param timeoutMs Timeout in milliseconds
     * @return Command result
     * @throws IllegalStateException if not connected
     */
    public CommandResult sendCommand(String command, int timeoutMs) {
        checkConnected();
        
        log.debug("Sending command: %s", command);
        
        String response = nativeSendCommandWithTimeout(command, timeoutMs);
        if (response == null) {
            return CommandResult.error(nativeGetLastError());
        }
        
        return CommandResult.parse(response);
    }
    
    /**
     * Reads memory from the target.
     * 
     * @param address Start address
     * @param length Number of bytes
     * @return Memory contents
     * @throws IllegalStateException if not connected
     * @throws NativeException if read fails
     */
    public byte[] readMemory(long address, int length) {
        checkConnected();
        
        byte[] data = nativeReadMemory(address, length);
        if (data == null) {
            throw new NativeException("Memory read failed: " + nativeGetLastError());
        }
        
        return data;
    }
    
    /**
     * Writes memory to the target.
     * 
     * @param address Start address
     * @param data Data to write
     * @throws IllegalStateException if not connected
     * @throws NativeException if write fails
     */
    public void writeMemory(long address, byte[] data) {
        checkConnected();
        
        int written = nativeWriteMemory(address, data);
        if (written < 0) {
            throw new NativeException("Memory write failed: " + nativeGetLastError());
        }
        
        if (written != data.length) {
            throw new NativeException(
                String.format("Partial write: expected %d bytes, wrote %d", data.length, written)
            );
        }
    }
    
    /**
     * Configures chipset-specific settings.
     * 
     * @param chipsetId Chipset identifier
     * @param config Chipset configuration
     * @throws NativeException if configuration fails
     */
    public void configureChipset(String chipsetId, ChipsetConfig config) {
        checkLibraryLoaded();
        
        String configJson = config.toJson();
        int result = nativeConfigureChipset(chipsetId, configJson);
        
        if (result != 0) {
            throw new NativeException("Chipset configuration failed: " + nativeGetLastError());
        }
        
        log.info("Configured chipset: %s", chipsetId);
    }
    
    /**
     * Gets the native library version.
     * 
     * @return Version string
     */
    public String getNativeVersion() {
        return nativeGetVersion();
    }
    
    /**
     * Gets the last error from native code.
     * 
     * @return Error message
     */
    public String getLastError() {
        return nativeGetLastError();
    }
    
    // ==================== HELPER METHODS ====================
    
    private void checkLibraryLoaded() {
        if (!libraryLoaded) {
            throw new IllegalStateException("Native library not loaded");
        }
    }
    
    private void checkConnected() {
        checkLibraryLoaded();
        if (!connected) {
            throw new IllegalStateException("Not connected to any probe");
        }
    }
}
