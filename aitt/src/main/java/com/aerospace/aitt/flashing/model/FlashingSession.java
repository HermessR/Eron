package com.aerospace.aitt.flashing.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a flashing session containing all modules to be flashed,
 * target profile, and execution state.
 */
public record FlashingSession(
    UUID id,
    List<SoftwareModule> modules,
    TargetProfile targetProfile,
    Path outputScriptPath,
    Instant timestamp,
    FlashStatus status,
    String errorMessage,
    List<String> logs,
    String remoteProbeId
) {
    /**
     * Creates a new FlashingSession with validation.
     */
    public FlashingSession {
        if (id == null) id = UUID.randomUUID();
        if (modules == null) modules = List.of();
        if (timestamp == null) timestamp = Instant.now();
        if (status == null) status = FlashStatus.PENDING;
        if (logs == null) logs = new ArrayList<>();
        
        // Make modules immutable
        modules = List.copyOf(modules);
    }
    
    /**
     * Creates a new session from modules and target.
     */
    public static FlashingSession create(List<SoftwareModule> modules, TargetProfile targetProfile) {
        return new FlashingSession(
            UUID.randomUUID(),
            modules,
            targetProfile,
            null,
            Instant.now(),
            FlashStatus.PENDING,
            null,
            new ArrayList<>(),
            null
        );
    }
    
    /**
     * Creates a new session with updated status.
     */
    public FlashingSession withStatus(FlashStatus newStatus) {
        return new FlashingSession(
            id, modules, targetProfile, outputScriptPath,
            timestamp, newStatus, errorMessage, logs, remoteProbeId
        );
    }
    
    /**
     * Creates a new session with output path.
     */
    public FlashingSession withOutputPath(Path path) {
        return new FlashingSession(
            id, modules, targetProfile, path,
            timestamp, status, errorMessage, logs, remoteProbeId
        );
    }
    
    /**
     * Creates a new session with error.
     */
    public FlashingSession withError(String error) {
        return new FlashingSession(
            id, modules, targetProfile, outputScriptPath,
            timestamp, FlashStatus.FAILED, error, logs, remoteProbeId
        );
    }
    
    /**
     * Creates a new session with an added log entry.
     */
    public FlashingSession withLog(String logEntry) {
        List<String> newLogs = new ArrayList<>(logs);
        newLogs.add(logEntry);
        return new FlashingSession(
            id, modules, targetProfile, outputScriptPath,
            timestamp, status, errorMessage, newLogs, remoteProbeId
        );
    }
    
    /**
     * Creates a new session with remote probe ID.
     */
    public FlashingSession withRemoteProbe(String probeId) {
        return new FlashingSession(
            id, modules, targetProfile, outputScriptPath,
            timestamp, status, errorMessage, logs, probeId
        );
    }
    
    /**
     * Returns true if this is a remote flashing session.
     */
    public boolean isRemote() {
        return remoteProbeId != null && !remoteProbeId.isEmpty();
    }
    
    /**
     * Returns the number of modules.
     */
    public int moduleCount() {
        return modules.size();
    }
    
    /**
     * Returns the total size of all modules.
     */
    public long totalSize() {
        return modules.stream().mapToLong(SoftwareModule::size).sum();
    }
    
    /**
     * Checks if the session was successful.
     */
    public boolean isSuccess() {
        return status == FlashStatus.SUCCESS;
    }
    
    /**
     * Checks if the session failed.
     */
    public boolean isFailed() {
        return status == FlashStatus.FAILED;
    }
    
    /**
     * Checks if the session is still running.
     */
    public boolean isRunning() {
        return status == FlashStatus.VALIDATING || 
               status == FlashStatus.GENERATING || 
               status == FlashStatus.TRANSFERRING ||
               status == FlashStatus.EXECUTING;
    }
    
    /**
     * Returns summary string for display.
     */
    public String summary() {
        return String.format("%d modules, %s", 
            moduleCount(),
            status.displayName()
        );
    }
}
