package com.aerospace.aitt.remote.model;

import java.time.Instant;

/**
 * Represents a network-attached debug probe.
 * 
 * <p>Network probes are TRACE32 PowerDebug units connected via ethernet
 * or remote machines hosting debug hardware accessible over TCP/IP.</p>
 * 
 * @param id Unique identifier for this probe
 * @param name Human-readable name
 * @param host IP address or hostname
 * @param port TCP port (default 20000 for TRACE32)
 * @param type Type of remote connection
 * @param status Current connection status
 * @param lastSeen Last time probe was detected/responded
 * @param firmware Firmware version if available
 * @param serialNumber Hardware serial number if available
 */
public record NetworkProbe(
    String id,
    String name,
    String host,
    int port,
    RemoteType type,
    ConnectionStatus status,
    Instant lastSeen,
    String firmware,
    String serialNumber
) {
    
    /** Default TRACE32 remote API port */
    public static final int DEFAULT_T32_PORT = 20000;
    
    /** Default probe server port for AITT remote service */
    public static final int DEFAULT_AITT_PORT = 20100;
    
    /**
     * Types of remote probe connections.
     */
    public enum RemoteType {
        /** TRACE32 PowerDebug with ethernet interface */
        T32_POWERDEBUG("TRACE32 PowerDebug"),
        
        /** Remote TRACE32 instance via TRACE32 Remote API */
        T32_REMOTE_API("TRACE32 Remote API"),
        
        /** AITT Remote Probe Server */
        AITT_PROBE_SERVER("AITT Probe Server"),
        
        /** Generic TCP debug bridge */
        TCP_DEBUG_BRIDGE("TCP Debug Bridge"),
        
        /** Unknown remote type */
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        RemoteType(String displayName) {
            this.displayName = displayName;
        }
        
        public String displayName() {
            return displayName;
        }
    }
    
    /**
     * Connection status for network probes.
     */
    public enum ConnectionStatus {
        /** Probe discovered but not connected */
        DISCOVERED("Discovered"),
        
        /** Currently connecting */
        CONNECTING("Connecting..."),
        
        /** Connected and ready */
        CONNECTED("Connected"),
        
        /** Connection busy with another operation */
        BUSY("Busy"),
        
        /** Connection failed or lost */
        DISCONNECTED("Disconnected"),
        
        /** Probe not responding */
        UNREACHABLE("Unreachable"),
        
        /** Authentication required */
        AUTH_REQUIRED("Auth Required"),
        
        /** Error state */
        ERROR("Error");
        
        private final String displayName;
        
        ConnectionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String displayName() {
            return displayName;
        }
        
        public boolean isAvailable() {
            return this == DISCOVERED || this == CONNECTED;
        }
    }
    
    /**
     * Creates a new probe with discovered status.
     */
    public static NetworkProbe discovered(String host, int port, RemoteType type) {
        String id = generateId(host, port);
        String name = type.displayName() + " @ " + host;
        return new NetworkProbe(
            id, name, host, port, type,
            ConnectionStatus.DISCOVERED,
            Instant.now(), null, null
        );
    }
    
    /**
     * Creates a probe from manual configuration.
     */
    public static NetworkProbe manual(String name, String host, int port, RemoteType type) {
        String id = generateId(host, port);
        return new NetworkProbe(
            id, name, host, port, type,
            ConnectionStatus.DISCOVERED,
            Instant.now(), null, null
        );
    }
    
    /**
     * Generates a unique ID for a probe.
     */
    private static String generateId(String host, int port) {
        return "net:" + host + ":" + port;
    }
    
    /**
     * Returns a copy with updated status.
     */
    public NetworkProbe withStatus(ConnectionStatus newStatus) {
        return new NetworkProbe(id, name, host, port, type, newStatus, Instant.now(), firmware, serialNumber);
    }
    
    /**
     * Returns a copy with updated firmware info.
     */
    public NetworkProbe withFirmware(String newFirmware, String newSerial) {
        return new NetworkProbe(id, name, host, port, type, status, lastSeen, newFirmware, newSerial);
    }
    
    /**
     * Returns the full connection address.
     */
    public String address() {
        return host + ":" + port;
    }
    
    /**
     * Returns display string for UI.
     */
    public String displayString() {
        return String.format("%s [%s] - %s", name, address(), status.displayName());
    }
    
    @Override
    public String toString() {
        return displayString();
    }
}
