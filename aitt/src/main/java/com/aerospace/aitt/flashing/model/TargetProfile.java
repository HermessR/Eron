package com.aerospace.aitt.flashing.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a target chipset profile with memory map and configuration.
 * Profiles are loaded from JSON and are not editable by users.
 */
public record TargetProfile(
    String id,
    String name,
    String cpuType,
    long flashStart,
    long flashEnd,
    long ramAddress,
    Path t32ConfigPath,
    String description
) {
    /**
     * Creates a TargetProfile with validation.
     */
    public TargetProfile {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(cpuType, "CPU type cannot be null");
        
        if (flashStart < 0) {
            throw new IllegalArgumentException("Flash start cannot be negative");
        }
        if (flashEnd <= flashStart) {
            throw new IllegalArgumentException("Flash end must be greater than start");
        }
        if (ramAddress < 0) {
            throw new IllegalArgumentException("RAM address cannot be negative");
        }
    }
    
    /**
     * Returns the total flash size in bytes.
     */
    public long flashSize() {
        return flashEnd - flashStart;
    }
    
    /**
     * Returns human-readable flash size.
     */
    public String formattedFlashSize() {
        long size = flashSize();
        if (size >= 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else if (size >= 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        return size + " B";
    }
    
    /**
     * Checks if an address is within the flash region.
     */
    public boolean containsAddress(long address) {
        return address >= flashStart && address < flashEnd;
    }
    
    /**
     * Checks if a memory region is within the flash boundaries.
     */
    public boolean containsRegion(long start, long size) {
        return start >= flashStart && (start + size) <= flashEnd;
    }
    
    /**
     * Returns hex-formatted flash start address.
     */
    public String formattedFlashStart() {
        return String.format("0x%08X", flashStart);
    }
    
    /**
     * Returns hex-formatted flash end address.
     */
    public String formattedFlashEnd() {
        return String.format("0x%08X", flashEnd);
    }
    
    /**
     * Returns hex-formatted RAM address.
     */
    public String formattedRamAddress() {
        return String.format("0x%08X", ramAddress);
    }
    
    /**
     * Returns display string for UI combo box.
     */
    public String displayName() {
        return String.format("%s (%s)", name, cpuType);
    }
    
    @Override
    public String toString() {
        return displayName();
    }
}
