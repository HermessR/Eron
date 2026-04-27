package com.aerospace.aitt.native_;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a specific chipset.
 * 
 * <p>Supports chipsets:
 * <ul>
 *   <li>S32K148 - NXP ARM Cortex-M4F</li>
 *   <li>MPC5777M - NXP Power Architecture</li>
 * </ul>
 */
public class ChipsetConfig {
    
    private final String chipsetId;
    private final Map<String, Object> settings;
    
    private ChipsetConfig(String chipsetId) {
        this.chipsetId = chipsetId;
        this.settings = new HashMap<>();
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Creates configuration for S32K148.
     */
    public static ChipsetConfig forS32K148() {
        ChipsetConfig config = new ChipsetConfig("S32K148");
        // Default S32K148 settings
        config.settings.put("cpu", "CortexM4");
        config.settings.put("flash_start", 0x00000000L);
        config.settings.put("flash_end", 0x00180000L);  // 1.5MB
        config.settings.put("ram_start", 0x1FFF8000L);
        config.settings.put("ram_end", 0x20007000L);    // 60KB
        config.settings.put("debug_interface", "SWD");
        config.settings.put("clock_speed", 112_000_000); // 112 MHz
        return config;
    }
    
    /**
     * Creates configuration for MPC5777M.
     */
    public static ChipsetConfig forMPC5777M() {
        ChipsetConfig config = new ChipsetConfig("MPC5777M");
        // Default MPC5777M settings
        config.settings.put("cpu", "e200z7");
        config.settings.put("flash_start", 0x00F90000L);
        config.settings.put("flash_end", 0x01780000L);  // Flash array
        config.settings.put("ram_start", 0x40000000L);
        config.settings.put("ram_end", 0x40060000L);    // 384KB
        config.settings.put("debug_interface", "JTAG");
        config.settings.put("clock_speed", 300_000_000); // 300 MHz
        return config;
    }
    
    /**
     * Creates custom chipset configuration.
     */
    public static ChipsetConfig custom(String chipsetId) {
        return new ChipsetConfig(chipsetId);
    }
    
    // ==================== BUILDER METHODS ====================
    
    public ChipsetConfig withCpu(String cpu) {
        settings.put("cpu", cpu);
        return this;
    }
    
    public ChipsetConfig withFlashRange(long start, long end) {
        settings.put("flash_start", start);
        settings.put("flash_end", end);
        return this;
    }
    
    public ChipsetConfig withRamRange(long start, long end) {
        settings.put("ram_start", start);
        settings.put("ram_end", end);
        return this;
    }
    
    public ChipsetConfig withDebugInterface(String iface) {
        settings.put("debug_interface", iface);
        return this;
    }
    
    public ChipsetConfig withClockSpeed(int hz) {
        settings.put("clock_speed", hz);
        return this;
    }
    
    public ChipsetConfig withSetting(String key, Object value) {
        settings.put(key, value);
        return this;
    }
    
    // ==================== ACCESSORS ====================
    
    public String getChipsetId() {
        return chipsetId;
    }
    
    public Object getSetting(String key) {
        return settings.get(key);
    }
    
    public long getFlashStart() {
        return ((Number) settings.getOrDefault("flash_start", 0L)).longValue();
    }
    
    public long getFlashEnd() {
        return ((Number) settings.getOrDefault("flash_end", 0L)).longValue();
    }
    
    /**
     * Converts configuration to JSON for native code.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"chipset_id\":\"").append(chipsetId).append("\"");
        
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            sb.append(",\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
}
