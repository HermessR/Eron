package com.aerospace.aitt.backend.hardware;

import java.io.IOException;

/**
 * Interface for hardware abstraction layer.
 * 
 * Resolves logical input/output names to hardware-specific addresses and encodings.
 * Implementations are chipset-specific (MPC5777M, S32K148, etc.).
 */
public interface HardwareAbstractionLayer {
    
    /**
     * Resolves a logical input/output name to its hardware address.
     * 
     * @param logicalName Logical identifier (e.g., "temperature_sensor")
     * @param dataType UI data type (e.g., "int (signed)", "boolean", "float")
     * @return Hardware address for this logical name
     * @throws IOException if address resolution fails
     */
    long resolveAddress(String logicalName, String dataType) throws IOException;
    
    /**
     * Resolves a logical name to its expected encoding format for CMM.
     * 
     * @param logicalName Logical identifier
     * @param dataType UI data type
     * @return CMM format specifier (e.g., "sint32", "uint32", "float32", "bit")
     */
    String resolveFormat(String logicalName, String dataType);
    
    /**
     * Validates that a logical name exists in the hardware configuration.
     * 
     * @param logicalName Logical identifier
     * @return true if name is valid and resolvable
     */
    boolean isValidLogicalName(String logicalName);
    
    /**
     * Gets the target chipset type.
     * 
     * @return Chipset identifier (e.g., "MPC5777M", "S32K148")
     */
    String getChipsetType();
}
