package com.aerospace.aitt.flashing.model;

import com.aerospace.aitt.core.util.ChecksumUtil;
import com.aerospace.aitt.core.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a firmware binary module to be flashed.
 * Contains file path, flash address, size, and checksum information.
 */
public record SoftwareModule(
    UUID id,
    String name,
    Path filePath,
    long flashAddress,
    long size,
    String checksum
) {
    /**
     * Creates a new SoftwareModule with validation.
     */
    public SoftwareModule {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(filePath, "File path cannot be null");
        
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (flashAddress < 0) {
            throw new IllegalArgumentException("Flash address cannot be negative");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative");
        }
    }
    
    /**
     * Creates a SoftwareModule from a file path and flash address.
     * Automatically calculates size and checksum.
     */
    public static SoftwareModule fromFile(Path filePath, long flashAddress) throws IOException {
        if (!FileUtil.isValidFile(filePath)) {
            throw new IOException("Invalid file: " + filePath);
        }
        
        String name = FileUtil.getBaseName(filePath);
        long size = FileUtil.getFileSize(filePath);
        String checksum = ChecksumUtil.crc32Hex(filePath);
        
        return new SoftwareModule(
            UUID.randomUUID(),
            name,
            filePath,
            flashAddress,
            size,
            checksum
        );
    }
    
    /**
     * Creates a SoftwareModule with a new flash address.
     */
    public SoftwareModule withFlashAddress(long newAddress) {
        return new SoftwareModule(id, name, filePath, newAddress, size, checksum);
    }
    
    /**
     * Returns the end address (exclusive) of this module in flash.
     */
    public long endAddress() {
        return flashAddress + size;
    }
    
    /**
     * Checks if this module overlaps with another.
     */
    public boolean overlapsWith(SoftwareModule other) {
        return this.flashAddress < other.endAddress() && 
               other.flashAddress < this.endAddress();
    }
    
    /**
     * Checks if this module is within the given memory region.
     */
    public boolean isWithinRegion(long regionStart, long regionEnd) {
        return flashAddress >= regionStart && endAddress() <= regionEnd;
    }
    
    /**
     * Returns the file name without path.
     */
    public String fileName() {
        return filePath.getFileName().toString();
    }
    
    /**
     * Returns the file extension.
     */
    public String fileExtension() {
        return FileUtil.getExtension(filePath);
    }
    
    /**
     * Returns human-readable size.
     */
    public String formattedSize() {
        return FileUtil.formatFileSize(size);
    }
    
    /**
     * Returns hex-formatted flash address.
     */
    public String formattedAddress() {
        return String.format("0x%08X", flashAddress);
    }
}
