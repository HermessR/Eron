package com.aerospace.aitt.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

/**
 * File utility methods for the application.
 */
public final class FileUtil {
    
    private FileUtil() {}
    
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.##");
    
    /**
     * Checks if a file exists and is readable.
     */
    public static boolean isValidFile(Path path) {
        return path != null && Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }
    
    /**
     * Gets the file size in bytes.
     */
    public static long getFileSize(Path path) throws IOException {
        return Files.size(path);
    }
    
    /**
     * Formats a file size in human-readable format.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) return "0 B";
        
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < SIZE_UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return SIZE_FORMAT.format(size) + " " + SIZE_UNITS[unitIndex];
    }
    
    /**
     * Gets the file extension.
     */
    public static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }
    
    /**
     * Gets the file name without extension.
     */
    public static String getBaseName(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
    
    /**
     * Checks if a file is a valid firmware file based on extension.
     */
    public static boolean isFirmwareFile(Path path) {
        String ext = getExtension(path);
        return ext.equals("bin") || ext.equals("hex") || ext.equals("elf") || ext.equals("srec");
    }
    
    /**
     * Creates parent directories if they don't exist.
     */
    public static void ensureParentExists(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
    
    /**
     * Reads file content as bytes.
     */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }
    
    /**
     * Writes bytes to a file.
     */
    public static void writeBytes(Path path, byte[] data) throws IOException {
        ensureParentExists(path);
        Files.write(path, data);
    }
    
    /**
     * Writes string content to a file.
     */
    public static void writeString(Path path, String content) throws IOException {
        ensureParentExists(path);
        Files.writeString(path, content);
    }
}
