package com.aerospace.aitt.core.util;

import java.util.regex.Pattern;

/**
 * Hexadecimal utility methods for address manipulation.
 */
public final class HexUtil {
    
    private HexUtil() {}
    
    private static final Pattern HEX_PATTERN = Pattern.compile("^(0[xX])?[0-9a-fA-F]+$");
    
    /**
     * Parses a hexadecimal string to a long value.
     * Accepts formats: "0x08000000", "08000000", "0X08000000"
     */
    public static long parseHex(String hex) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException("Hex string cannot be null or empty");
        }
        
        String cleaned = hex.trim();
        if (cleaned.toLowerCase().startsWith("0x")) {
            cleaned = cleaned.substring(2);
        }
        
        return Long.parseUnsignedLong(cleaned, 16);
    }
    
    /**
     * Formats a long value as a hexadecimal string with 0x prefix.
     */
    public static String toHex(long value) {
        return String.format("0x%08X", value);
    }
    
    /**
     * Formats a long value as a hexadecimal string with specified width.
     */
    public static String toHex(long value, int width) {
        return String.format("0x%0" + width + "X", value);
    }
    
    /**
     * Checks if a string is a valid hexadecimal number.
     */
    public static boolean isValidHex(String hex) {
        if (hex == null || hex.isBlank()) {
            return false;
        }
        return HEX_PATTERN.matcher(hex.trim()).matches();
    }
    
    /**
     * Checks if an address is aligned to the specified boundary.
     */
    public static boolean isAligned(long address, int alignment) {
        return (address % alignment) == 0;
    }
    
    /**
     * Aligns an address up to the specified boundary.
     */
    public static long alignUp(long address, int alignment) {
        return ((address + alignment - 1) / alignment) * alignment;
    }
    
    /**
     * Aligns an address down to the specified boundary.
     */
    public static long alignDown(long address, int alignment) {
        return (address / alignment) * alignment;
    }
    
    /**
     * Calculates the end address given start address and size.
     */
    public static long endAddress(long startAddress, long size) {
        return startAddress + size - 1;
    }
    
    /**
     * Checks if two memory regions overlap.
     */
    public static boolean regionsOverlap(long start1, long size1, long start2, long size2) {
        long end1 = start1 + size1;
        long end2 = start2 + size2;
        return start1 < end2 && start2 < end1;
    }
    
    /**
     * Checks if a region is contained within a boundary.
     */
    public static boolean isWithinBoundary(long regionStart, long regionSize, 
                                           long boundaryStart, long boundaryEnd) {
        long regionEnd = regionStart + regionSize;
        return regionStart >= boundaryStart && regionEnd <= boundaryEnd;
    }
}
