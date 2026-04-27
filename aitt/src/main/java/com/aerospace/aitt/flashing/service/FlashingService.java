package com.aerospace.aitt.flashing.service;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.flashing.data.ProfileStore;
import com.aerospace.aitt.flashing.data.SessionStore;
import com.aerospace.aitt.flashing.model.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Main orchestrator service for flashing operations.
 * Coordinates validation, script generation, and TRACE32 execution.
 */
public class FlashingService {
    
    private final AppLogger log = new AppLogger(FlashingService.class);
    
    private final Validator validator;
    private final CmmGenerator cmmGenerator;
    private final Trace32Runner trace32Runner;
    private final SessionStore sessionStore;
    private final ProfileStore profileStore;
    private final ExecutorService executor;
    
    private volatile boolean abortRequested = false;
    private volatile FlashingSession currentSession = null;
    
    public FlashingService(SessionStore sessionStore, ProfileStore profileStore) {
        this.validator = new Validator();
        this.cmmGenerator = new CmmGenerator();
        this.trace32Runner = new Trace32Runner();
        this.sessionStore = sessionStore;
        this.profileStore = profileStore;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "flashing-worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Validates the firmware modules against the target profile.
     */
    public ValidationResult validate(List<SoftwareModule> modules, TargetProfile target) {
        return validator.validate(modules, target);
    }
    
    /**
     * Executes the flashing process asynchronously.
     *
     * @param modules List of firmware modules to flash
     * @param target Target chipset profile
     * @param outputPath Path to save the generated CMM script
     * @param runTrace32 Whether to launch TRACE32 after generation
     * @param trace32Path Path to TRACE32 executable
     * @param logCallback Callback for log messages
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return CompletableFuture with the flashing session result
     */
    public CompletableFuture<FlashingSession> execute(
            List<SoftwareModule> modules,
            TargetProfile target,
            Path outputPath,
            boolean runTrace32,
            Path trace32Path,
            Consumer<String> logCallback,
            Consumer<Double> progressCallback) {
        
        abortRequested = false;
        
        // Null-safe callbacks
        final Consumer<String> safeLogCallback = logCallback != null ? logCallback : s -> {};
        final Consumer<Double> safeProgressCallback = progressCallback != null ? progressCallback : d -> {};
        
        return CompletableFuture.supplyAsync(() -> {
            FlashingSession session = FlashingSession.create(modules, target)
                .withOutputPath(outputPath);
            currentSession = session;
            
            try {
                // Step 1: Validate (10%)
                session = session.withStatus(FlashStatus.VALIDATING);
                safeProgressCallback.accept(0.1);
                safeLogCallback.accept("Validating firmware files and addresses...");
                
                ValidationResult validation = validator.validate(modules, target);
                if (!validation.valid()) {
                    String errorMsg = "Validation failed:\\n" + validation.errorSummary();
                    safeLogCallback.accept("ERROR: " + errorMsg);
                    session = session.withError(errorMsg);
                    sessionStore.save(session);
                    return session;
                }
                
                safeLogCallback.accept("Validation passed: " + modules.size() + " modules verified");
                checkAbort();
                
                // Step 2: Generate CMM script (30%)
                session = session.withStatus(FlashStatus.GENERATING);
                safeProgressCallback.accept(0.3);
                safeLogCallback.accept("Generating TRACE32 CMM script...");
                
                String script = cmmGenerator.generate(session);
                cmmGenerator.saveTo(outputPath, script);
                
                safeLogCallback.accept("Script saved to: " + outputPath);
                checkAbort();
                
                // Step 3: Run TRACE32 (50% - 100%)
                if (runTrace32) {
                    session = session.withStatus(FlashStatus.EXECUTING);
                    safeProgressCallback.accept(0.5);
                    safeLogCallback.accept("Launching TRACE32...");
                    
                    Path configPath = target.t32ConfigPath();
                    int exitCode = trace32Runner.run(trace32Path, configPath, outputPath, msg -> {
                        safeLogCallback.accept(msg);
                    });
                    
                    if (abortRequested) {
                        safeLogCallback.accept("Flashing aborted by user");
                        session = session.withStatus(FlashStatus.ABORTED);
                    } else if (exitCode != 0) {
                        String errorMsg = "TRACE32 exited with code: " + exitCode;
                        safeLogCallback.accept("ERROR: " + errorMsg);
                        session = session.withError(errorMsg);
                    } else {
                        safeProgressCallback.accept(1.0);
                        safeLogCallback.accept("Flashing completed successfully");
                        session = session.withStatus(FlashStatus.SUCCESS);
                    }
                } else {
                    // Just generate script, don't run
                    safeProgressCallback.accept(1.0);
                    safeLogCallback.accept("CMM script generated successfully (TRACE32 not launched)");
                    session = session.withStatus(FlashStatus.SUCCESS);
                }
                
            } catch (AbortException e) {
                safeLogCallback.accept("Flashing aborted by user");
                session = session.withStatus(FlashStatus.ABORTED);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                safeLogCallback.accept("ERROR: " + errorMsg);
                log.error("Flashing failed", e);
                session = session.withError(errorMsg);
            }
            
            // Save session to history
            try {
                sessionStore.save(session);
            } catch (Exception e) {
                log.error("Failed to save session", e);
            }
            
            currentSession = null;
            return session;
            
        }, executor);
    }
    
    /**
     * Executes the flashing process asynchronously with simplified callback.
     * Uses default TRACE32 path from profile configuration.
     *
     * @param modules List of firmware modules to flash
     * @param target Target chipset profile
     * @param outputPath Path to save the generated CMM script
     * @param runTrace32 Whether to launch TRACE32 after generation
     * @param statusCallback Callback for status messages
     * @param logCallback Callback for log messages
     * @return CompletableFuture with the flashing session result
     */
    public CompletableFuture<FlashingSession> executeAsync(
            List<SoftwareModule> modules,
            TargetProfile target,
            Path outputPath,
            boolean runTrace32,
            Consumer<String> statusCallback,
            Consumer<String> logCallback) {
        
        // Get TRACE32 path from config or use default
        Path trace32Path = Trace32Runner.findTrace32Executable();
        
        // Wrap status callback as a combined log
        Consumer<String> combinedLog = msg -> {
            logCallback.accept(msg);
        };
        
        // Create a progress callback that updates status
        Consumer<Double> progressCallback = progress -> {
            String status = switch ((int)(progress * 100)) {
                case 10 -> "Validating...";
                case 30 -> "Generating CMM...";
                case 50 -> "Executing TRACE32...";
                case 100 -> "Complete";
                default -> "Processing...";
            };
            statusCallback.accept(status);
        };
        
        return execute(modules, target, outputPath, runTrace32, trace32Path, combinedLog, progressCallback);
    }
    
    /**
     * Aborts the current flashing operation.
     */
    public void abort() {
        log.info("Abort requested");
        abortRequested = true;
        trace32Runner.kill();
    }
    
    /**
     * Returns the current session if one is running.
     */
    public FlashingSession getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Checks if a flashing operation is in progress.
     */
    public boolean isRunning() {
        return currentSession != null && currentSession.isRunning();
    }
    
    /**
     * Returns all available target profiles.
     */
    public List<TargetProfile> getAvailableProfiles() {
        return profileStore.findAll();
    }
    
    /**
     * Returns all saved flashing sessions.
     */
    public List<FlashingSession> getSessionHistory() {
        return sessionStore.findAll();
    }
    
    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    private void checkAbort() throws AbortException {
        if (abortRequested) {
            abortRequested = false;
            throw new AbortException();
        }
    }
    
    /**
     * Exception thrown when abort is requested.
     */
    private static class AbortException extends Exception {
        public AbortException() {
            super("Operation aborted");
        }
    }
}
