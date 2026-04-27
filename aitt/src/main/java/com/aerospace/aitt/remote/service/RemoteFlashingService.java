package com.aerospace.aitt.remote.service;

import com.aerospace.aitt.core.log.AppLogger;
import com.aerospace.aitt.flashing.model.*;
import com.aerospace.aitt.flashing.service.CmmGenerator;
import com.aerospace.aitt.flashing.service.Validator;
import com.aerospace.aitt.remote.model.NetworkProbe;
import com.aerospace.aitt.remote.model.NetworkProbe.ConnectionStatus;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for executing flashing operations on remote/network-attached probes.
 * 
 * <p>This service coordinates with remote AITT probe servers or TRACE32 
 * instances to execute flashing operations over the network.</p>
 * 
 * <h2>Remote Flashing Protocol</h2>
 * <p>Communication with remote servers uses a simple text-based protocol:</p>
 * <ul>
 *   <li>FLASH_START:target:modulecount - Initiates a flashing session</li>
 *   <li>FLASH_MODULE:name:address:size:base64data - Transfers a firmware module</li>
 *   <li>FLASH_SCRIPT:base64script - Transfers the CMM script</li>
 *   <li>FLASH_EXECUTE - Starts the flashing process</li>
 *   <li>FLASH_ABORT - Aborts the current operation</li>
 *   <li>FLASH_STATUS - Queries current status</li>
 * </ul>
 */
public class RemoteFlashingService implements AutoCloseable {
    
    private static final AppLogger log = new AppLogger(RemoteFlashingService.class);
    
    private static final int TRANSFER_TIMEOUT_MS = 60000;
    private static final int EXECUTE_TIMEOUT_MS = 300000; // 5 minutes
    private static final int CHUNK_SIZE = 65536; // 64KB chunks for file transfer
    
    private final NetworkProbeService probeService;
    private final Validator validator;
    private final CmmGenerator cmmGenerator;
    private final ExecutorService executor;
    
    private volatile boolean abortRequested = false;
    private volatile FlashingSession currentSession = null;
    
    public RemoteFlashingService(NetworkProbeService probeService) {
        this.probeService = probeService;
        this.validator = new Validator();
        this.cmmGenerator = new CmmGenerator();
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "remote-flash-worker");
            t.setDaemon(true);
            return t;
        });
        
        log.info("RemoteFlashingService initialized");
    }
    
    /**
     * Validates firmware modules against target profile.
     */
    public ValidationResult validate(List<SoftwareModule> modules, TargetProfile target) {
        return validator.validate(modules, target);
    }
    
    /**
     * Executes remote flashing operation.
     *
     * @param probe Target network probe
     * @param modules List of firmware modules to flash
     * @param target Target chipset profile
     * @param logCallback Callback for log messages
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return CompletableFuture with the flashing session result
     */
    public CompletableFuture<FlashingSession> executeRemote(
            NetworkProbe probe,
            List<SoftwareModule> modules,
            TargetProfile target,
            Consumer<String> logCallback,
            Consumer<Double> progressCallback) {
        
        abortRequested = false;
        
        return CompletableFuture.supplyAsync(() -> {
            FlashingSession session = FlashingSession.create(modules, target)
                .withRemoteProbe(probe.id());
            currentSession = session;
            
            try {
                // Check probe connection
                if (!probeService.isConnected(probe)) {
                    logCallback.accept("Connecting to remote probe...");
                    progressCallback.accept(0.05);
                    
                    boolean connected = probeService.connect(probe).get(10, TimeUnit.SECONDS);
                    if (!connected) {
                        throw new IOException("Failed to connect to remote probe: " + probe.address());
                    }
                }
                
                Socket socket = probeService.getConnection(probe)
                    .orElseThrow(() -> new IOException("No active connection to probe"));
                
                // Step 1: Validate (10%)
                session = session.withStatus(FlashStatus.VALIDATING);
                progressCallback.accept(0.1);
                logCallback.accept("Validating firmware files...");
                
                ValidationResult validation = validator.validate(modules, target);
                if (!validation.valid()) {
                    String errorMsg = "Validation failed:\n" + validation.errorSummary();
                    logCallback.accept("ERROR: " + errorMsg);
                    session = session.withError(errorMsg);
                    return session;
                }
                
                logCallback.accept("Validation passed: " + modules.size() + " modules verified");
                checkAbort();
                
                // Step 2: Generate CMM script (15%)
                progressCallback.accept(0.15);
                logCallback.accept("Generating TRACE32 CMM script...");
                
                String script = cmmGenerator.generate(session);
                checkAbort();
                
                // Step 3: Initialize remote session (20%)
                session = session.withStatus(FlashStatus.TRANSFERRING);
                progressCallback.accept(0.2);
                logCallback.accept("Initializing remote flashing session...");
                
                sendCommand(socket, String.format("FLASH_START:%s:%d", 
                    target.id(), modules.size()));
                String response = readResponse(socket);
                
                if (!response.startsWith("OK")) {
                    throw new IOException("Remote probe rejected session: " + response);
                }
                
                checkAbort();
                
                // Step 4: Transfer firmware modules (20% - 70%)
                double progressPerModule = 0.5 / modules.size();
                double currentProgress = 0.2;
                
                for (int i = 0; i < modules.size(); i++) {
                    SoftwareModule module = modules.get(i);
                    logCallback.accept("Transferring: " + module.name() + " (" + (i + 1) + "/" + modules.size() + ")");
                    
                    transferModule(socket, module, logCallback);
                    
                    currentProgress += progressPerModule;
                    progressCallback.accept(currentProgress);
                    checkAbort();
                }
                
                // Step 5: Transfer CMM script (75%)
                progressCallback.accept(0.75);
                logCallback.accept("Transferring CMM script...");
                
                String scriptBase64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
                sendCommand(socket, "FLASH_SCRIPT:" + scriptBase64);
                response = readResponse(socket);
                
                if (!response.startsWith("OK")) {
                    throw new IOException("Failed to transfer script: " + response);
                }
                
                checkAbort();
                
                // Step 6: Execute on remote (80% - 100%)
                session = session.withStatus(FlashStatus.EXECUTING);
                progressCallback.accept(0.8);
                logCallback.accept("Executing flashing on remote probe...");
                
                sendCommand(socket, "FLASH_EXECUTE");
                
                // Wait for execution with progress updates
                boolean completed = waitForExecution(socket, logCallback, progress -> {
                    progressCallback.accept(0.8 + (progress * 0.2));
                });
                
                if (abortRequested) {
                    sendCommand(socket, "FLASH_ABORT");
                    logCallback.accept("Flashing aborted by user");
                    session = session.withStatus(FlashStatus.ABORTED);
                } else if (completed) {
                    progressCallback.accept(1.0);
                    logCallback.accept("Remote flashing completed successfully");
                    session = session.withStatus(FlashStatus.SUCCESS);
                } else {
                    throw new IOException("Remote execution failed");
                }
                
            } catch (AbortException e) {
                logCallback.accept("Flashing aborted by user");
                session = session.withStatus(FlashStatus.ABORTED);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                logCallback.accept("ERROR: " + errorMsg);
                log.error("Remote flashing failed", e);
                session = session.withError(errorMsg);
            }
            
            currentSession = null;
            return session;
            
        }, executor);
    }
    
    /**
     * Transfers a firmware module to the remote probe.
     */
    private void transferModule(Socket socket, SoftwareModule module, Consumer<String> logCallback) 
            throws IOException {
        
        Path filePath = module.filePath();
        if (!Files.exists(filePath)) {
            throw new IOException("Firmware file not found: " + filePath);
        }
        
        byte[] data = Files.readAllBytes(filePath);
        String dataBase64 = Base64.getEncoder().encodeToString(data);
        
        // Send module header
        String command = String.format("FLASH_MODULE:%s:0x%X:%d:%s",
            module.name(),
            module.flashAddress(),
            data.length,
            dataBase64);
        
        sendCommand(socket, command);
        String response = readResponse(socket);
        
        if (!response.startsWith("OK")) {
            throw new IOException("Failed to transfer module " + module.name() + ": " + response);
        }
        
        logCallback.accept("  Transferred: " + module.name() + " (" + data.length + " bytes)");
    }
    
    /**
     * Waits for remote execution to complete, relaying progress.
     */
    private boolean waitForExecution(Socket socket, Consumer<String> logCallback, 
            Consumer<Double> progressCallback) throws IOException {
        
        long deadline = System.currentTimeMillis() + EXECUTE_TIMEOUT_MS;
        
        while (System.currentTimeMillis() < deadline && !abortRequested) {
            sendCommand(socket, "FLASH_STATUS");
            String status = readResponse(socket);
            
            if (status.startsWith("COMPLETE")) {
                return true;
            } else if (status.startsWith("ERROR:")) {
                throw new IOException(status.substring(6));
            } else if (status.startsWith("PROGRESS:")) {
                try {
                    String[] parts = status.split(":");
                    double progress = Double.parseDouble(parts[1]);
                    String message = parts.length > 2 ? parts[2] : "";
                    
                    progressCallback.accept(progress);
                    if (!message.isEmpty()) {
                        logCallback.accept("  Remote: " + message);
                    }
                } catch (NumberFormatException ignored) {}
            } else if (status.startsWith("LOG:")) {
                logCallback.accept("  Remote: " + status.substring(4));
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return false;
    }
    
    /**
     * Sends a command to the socket.
     */
    private void sendCommand(Socket socket, String command) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    
    /**
     * Reads a response from the socket.
     */
    private String readResponse(Socket socket) throws IOException {
        socket.setSoTimeout(TRANSFER_TIMEOUT_MS);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String line = reader.readLine();
        return line != null ? line : "";
    }
    
    /**
     * Requests abort of current operation.
     */
    public void abort() {
        abortRequested = true;
        log.info("Remote flashing abort requested");
    }
    
    /**
     * Checks if abort was requested and throws if so.
     */
    private void checkAbort() throws AbortException {
        if (abortRequested) {
            throw new AbortException();
        }
    }
    
    /**
     * Gets the current flashing session if any.
     */
    public FlashingSession getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Custom exception for abort handling.
     */
    private static class AbortException extends Exception {
        AbortException() {
            super("Operation aborted");
        }
    }
    
    @Override
    public void close() {
        abortRequested = true;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        
        log.info("RemoteFlashingService closed");
    }
}
