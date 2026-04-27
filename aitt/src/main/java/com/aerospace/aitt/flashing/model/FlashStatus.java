package com.aerospace.aitt.flashing.model;

/**
 * Represents the status of a flashing session.
 */
public enum FlashStatus {
    
    /**
     * Session created but not started.
     */
    PENDING("Pending", "Session ready to execute"),
    
    /**
     * Validating input files and addresses.
     */
    VALIDATING("Validating", "Validating firmware files and addresses"),
    
    /**
     * Generating CMM script.
     */
    GENERATING("Generating", "Generating TRACE32 script"),
    
    /**
     * Transferring files to remote probe.
     */
    TRANSFERRING("Transferring", "Transferring files to remote probe"),
    
    /**
     * Executing flashing via TRACE32.
     */
    EXECUTING("Executing", "Flashing in progress"),
    
    /**
     * Flashing completed successfully.
     */
    SUCCESS("Success", "Flashing completed successfully"),
    
    /**
     * Flashing failed.
     */
    FAILED("Failed", "Flashing failed"),
    
    /**
     * Flashing aborted by user.
     */
    ABORTED("Aborted", "Flashing aborted by user");
    
    private final String displayName;
    private final String description;
    
    FlashStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String displayName() {
        return displayName;
    }
    
    public String description() {
        return description;
    }
    
    /**
     * Checks if this status represents a terminal state.
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == ABORTED;
    }
    
    /**
     * Checks if this status represents an active/running state.
     */
    public boolean isRunning() {
        return this == VALIDATING || this == GENERATING || this == TRANSFERRING || this == EXECUTING;
    }
    
    /**
     * Returns CSS class for status styling.
     */
    public String cssClass() {
        return switch (this) {
            case SUCCESS -> "status-success";
            case FAILED -> "status-failed";
            case ABORTED -> "status-aborted";
            case EXECUTING, GENERATING, VALIDATING, TRANSFERRING -> "status-running";
            case PENDING -> "status-pending";
        };
    }
}
