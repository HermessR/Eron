package com.aerospace.aitt.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * Checksum and hash utility methods for file verification.
 */
public final class ChecksumUtil {
    
    private ChecksumUtil() {}
    
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Calculates SHA-256 hash of a file.
     */
    public static String sha256(Path file) throws IOException {
        return hash(file, "SHA-256");
    }
    
    /**
     * Calculates MD5 hash of a file.
     */
    public static String md5(Path file) throws IOException {
        return hash(file, "MD5");
    }
    
    /**
     * Calculates CRC32 checksum of a file.
     */
    public static long crc32(Path file) throws IOException {
        CRC32 crc = new CRC32();
        
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }
        
        return crc.getValue();
    }
    
    /**
     * Calculates CRC32 checksum and returns as hex string.
     */
    public static String crc32Hex(Path file) throws IOException {
        return String.format("%08X", crc32(file));
    }
    
    /**
     * Calculates hash of a file using specified algorithm.
     */
    private static String hash(Path file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            return bytesToHex(digest.digest());
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not available: " + algorithm, e);
        }
    }
    
    /**
     * Calculates SHA-256 hash of byte array.
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Converts byte array to hexadecimal string.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Verifies that a file matches the expected checksum.
     */
    public static boolean verify(Path file, String expectedChecksum) throws IOException {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return true; // No checksum to verify
        }
        
        String actualChecksum;
        actualChecksum = switch (expectedChecksum.length()) {
            case 8 -> crc32Hex(file);  // CRC32
            case 32 -> md5(file);       // MD5
            default -> sha256(file);    // SHA-256
        };
        
        return actualChecksum.equalsIgnoreCase(expectedChecksum);
    }
}
