package com.aerospace.aitt.native_;

/**
 * Represents detected hardware probe information.
 * 
 * <p>Parsed from native detection results in format: "TYPE:ID:NAME:STATUS"</p>
 * 
 * @param type Probe type (USB, SERIAL, JTAG, SWD)
 * @param id Unique identifier for connection
 * @param name Human-readable name
 * @param status Connection status (AVAILABLE, IN_USE, ERROR)
 */
public record HardwareInfo(
    ProbeType type,
    String id,
    String name,
    ProbeStatus status
) {
    /**
     * Probe connection types.
     */
    public enum ProbeType {
        USB("USB Debug Probe"),
        SERIAL("Serial/COM Port"),
        JTAG("JTAG Interface"),
        SWD("SWD Interface"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ProbeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String displayName() {
            return displayName;
        }
        
        public static ProbeType fromString(String s) {
            if (s == null) return UNKNOWN;
            return switch (s.toUpperCase()) {
                case "USB" -> USB;
                case "SERIAL", "COM" -> SERIAL;
                case "JTAG" -> JTAG;
                case "SWD" -> SWD;
                default -> UNKNOWN;
            };
        }
    }
    
    /**
     * Probe availability status.
     */
    public enum ProbeStatus {
        AVAILABLE("Available"),
        IN_USE("In Use"),
        ERROR("Error"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ProbeStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String displayName() {
            return displayName;
        }
        
        public static ProbeStatus fromString(String s) {
            if (s == null) return UNKNOWN;
            return switch (s.toUpperCase()) {
                case "AVAILABLE", "OK", "READY" -> AVAILABLE;
                case "IN_USE", "BUSY" -> IN_USE;
                case "ERROR", "FAIL" -> ERROR;
                default -> UNKNOWN;
            };
        }
    }
    
    /**
     * Parses hardware info from native string format.
     * Format: "TYPE:ID:NAME:STATUS"
     * 
     * @param raw Raw string from native code
     * @return Parsed HardwareInfo or null if invalid
     */
    public static HardwareInfo parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        
        String[] parts = raw.split(":", 4);
        if (parts.length < 2) {
            return null;
        }
        
        ProbeType type = ProbeType.fromString(parts[0]);
        String id = parts[1];
        String name = parts.length > 2 ? parts[2] : id;
        ProbeStatus status = parts.length > 3 
            ? ProbeStatus.fromString(parts[3]) 
            : ProbeStatus.AVAILABLE;
        
        return new HardwareInfo(type, id, name, status);
    }
    
    /**
     * Returns display string for UI.
     */
    public String displayName() {
        return String.format("%s - %s (%s)", type.displayName(), name, id);
    }
    
    /**
     * Checks if probe is available for connection.
     */
    public boolean isAvailable() {
        return status == ProbeStatus.AVAILABLE;
    }
    
    @Override
    public String toString() {
        return displayName();
    }
}
