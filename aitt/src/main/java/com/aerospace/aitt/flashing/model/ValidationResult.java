package com.aerospace.aitt.flashing.model;

import java.util.List;

/**
 * Result of firmware validation.
 */
public record ValidationResult(
    boolean valid,
    List<ValidationError> errors
) {
    /**
     * Creates a successful validation result.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }
    
    /**
     * Creates a failed validation result with errors.
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, List.copyOf(errors));
    }
    
    /**
     * Creates a failed validation result with a single error.
     */
    public static ValidationResult failure(ValidationError error) {
        return new ValidationResult(false, List.of(error));
    }
    
    /**
     * Creates a failed validation result with error message.
     */
    public static ValidationResult failure(ValidationErrorType type, String message) {
        return failure(new ValidationError(type, message, null));
    }
    
    /**
     * Returns error count.
     */
    public int errorCount() {
        return errors.size();
    }
    
    /**
     * Checks if there are errors of a specific type.
     */
    public boolean hasErrorType(ValidationErrorType type) {
        return errors.stream().anyMatch(e -> e.type() == type);
    }
    
    /**
     * Returns all error messages joined.
     */
    public String errorSummary() {
        return errors.stream()
            .map(ValidationError::message)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
    }
    
    /**
     * Validation error record.
     */
    public record ValidationError(
        ValidationErrorType type,
        String message,
        String moduleName
    ) {
        public ValidationError(ValidationErrorType type, String message) {
            this(type, message, null);
        }
    }
    
    /**
     * Types of validation errors.
     */
    public enum ValidationErrorType {
        FILE_NOT_FOUND("File not found"),
        FILE_NOT_READABLE("File not readable"),
        INVALID_ADDRESS("Invalid flash address"),
        ADDRESS_OVERLAP("Memory address overlap"),
        OUT_OF_BOUNDS("Address out of flash bounds"),
        SIZE_EXCEEDED("Module size exceeds available space"),
        INVALID_CHECKSUM("Checksum verification failed"),
        INVALID_FORMAT("Invalid file format"),
        DUPLICATE_ADDRESS("Duplicate flash address"),
        FORBIDDEN_ADDRESS("Forbidden address region");
        
        private final String displayName;
        
        ValidationErrorType(String displayName) {
            this.displayName = displayName;
        }
        
        public String displayName() {
            return displayName;
        }
    }
}
