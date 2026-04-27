package com.aerospace.aitt.testdev.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.aerospace.aitt.backend.hardware.CMMScriptGenerator;
import com.aerospace.aitt.backend.hardware.HardwareAbstractionLayer;
import com.aerospace.aitt.core.log.AppLogger;

/**
 * Service layer for CMM test script generation and execution.
 * 
 * Responsibilities:
 * - Manage test input/output definitions
 * - Generate CMM scripts via CMMScriptGenerator
 * - Execute scripts via Trace32 CLI
 * - Capture and parse execution logs
 * - Provide result comparison to external post-processor
 */
public class CMMTestService {
    
    private static final AppLogger log = new AppLogger(CMMTestService.class);
    
    private final CMMScriptGenerator scriptGenerator;
    private final HardwareAbstractionLayer hardwareLayer;
    private volatile Path lastGeneratedScript;
    private volatile Path lastExecutionLog;
    
    public CMMTestService(HardwareAbstractionLayer hardwareLayer) {
        this.hardwareLayer = hardwareLayer;
        this.scriptGenerator = new CMMScriptGenerator(hardwareLayer);
    }
    
    /**
     * Generates a CMM test script from user-provided test definition.
     * 
     * @param testName Descriptive test name
     * @param inputs List of test inputs with logical names and values
     * @param outputs List of expected outputs to capture
     * @param breakpointName Function/symbol where breakpoint should be set
     * @return Async task that completes with generated script path
     */
    public CompletableFuture<Path> generateTestScript(
            String testName,
            List<CMMScriptGenerator.TestInput> inputs,
            List<CMMScriptGenerator.TestOutput> outputs,
            String breakpointName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Generating CMM script for test: " + testName);
                
                // Validate inputs
                if (inputs == null || inputs.isEmpty()) {
                    throw new IllegalArgumentException("At least one input must be defined");
                }
                
                if (breakpointName == null || breakpointName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Breakpoint name must be specified");
                }
                
                // Validate logical names exist in hardware layer
                for (CMMScriptGenerator.TestInput input : inputs) {
                    if (!hardwareLayer.isValidLogicalName(input.getLogicalName())) {
                        throw new IllegalArgumentException(
                            "Invalid logical name: " + input.getLogicalName() +
                            ". Run getAvailableLogicalNames() for valid options."
                        );
                    }
                }
                
                // Create test definition
                CMMScriptGenerator.TestDefinition testDef = 
                    new CMMScriptGenerator.TestDefinition(
                        testName, 
                        inputs, 
                        outputs != null ? outputs : List.of(),
                        breakpointName
                    );
                
                // Generate script
                Path scriptPath = scriptGenerator.generateScript(testDef);
                lastGeneratedScript = scriptPath;
                
                log.info("CMM script generated successfully: " + scriptPath);
                return scriptPath;
                
            } catch (Exception e) {
                log.error("Failed to generate CMM script: " + e.getMessage(), e);
                throw new RuntimeException("Script generation failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Executes a CMM script via Trace32 CLI.
     * 
     * @param scriptPath Path to .cmm file to execute
     * @return Async task that completes with execution output log path
     */
    public CompletableFuture<Path> executeScript(Path scriptPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing CMM script: " + scriptPath);
                
                if (!scriptPath.toFile().exists()) {
                    throw new IOException("Script file not found: " + scriptPath);
                }
                
                // Run Trace32 with script
                // On Windows, typically: T32.exe -c <scriptPath>
                ProcessBuilder pb = new ProcessBuilder(
                    "T32.exe", 
                    "-c", 
                    scriptPath.toString()
                );
                
                pb.redirectErrorStream(true);
                
                // Set working directory for output
                pb.directory(scriptPath.getParent().toFile());
                
                // Start process
                Process process = pb.start();
                
                // Wait for completion (with timeout)
                boolean completed = process.waitFor(
                    60, // 60 second timeout
                    java.util.concurrent.TimeUnit.SECONDS
                );
                
                if (!completed) {
                    process.destroyForcibly();
                    throw new IOException("CMM script execution timed out after 60 seconds");
                }
                
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    log.warn("CMM script exited with code: " + exitCode);
                }
                
                // Locate generated log file (typically in aitt/logs/ directory)
                Path logPath = scriptPath.getParent()
                    .getParent()
                    .resolve("logs")
                    .resolve("test_execution_*.log");
                
                lastExecutionLog = logPath;
                log.info("CMM script execution complete");
                
                return logPath;
                
            } catch (Exception e) {
                log.error("Failed to execute CMM script: " + e.getMessage(), e);
                throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Validates a logical name exists in the hardware configuration.
     * 
     * @param logicalName Input/output logical identifier
     * @return true if valid
     */
    public boolean validateLogicalName(String logicalName) {
        return hardwareLayer.isValidLogicalName(logicalName);
    }
    
    /**
     * Gets all available logical names in the hardware configuration.
     * Used for UI autocomplete/dropdown population.
     * 
     * @return Set of valid logical names
     */
    public java.util.Set<String> getAvailableLogicalNames() {
        if (hardwareLayer instanceof com.aerospace.aitt.backend.hardware.MPCHardwareImpl) {
            return com.aerospace.aitt.backend.hardware.MPCHardwareImpl.getAvailableLogicalNames();
        }
        return new java.util.HashSet<>();
    }
    
    /**
     * Gets the resolved hardware address for a logical name.
     * 
     * @param logicalName Input/output identifier
     * @return Resolved hardware address
     * @throws IOException if resolution fails
     */
    public long getResolvedAddress(String logicalName) throws IOException {
        return hardwareLayer.resolveAddress(logicalName, "");
    }
    
    /**
     * Gets the resolved CMM format for a logical name.
     * 
     * @param logicalName Input/output identifier
     * @return CMM format specifier (e.g., "sint32", "uint32", "float32", "bit")
     */
    public String getResolvedFormat(String logicalName) {
        return hardwareLayer.resolveFormat(logicalName, "");
    }
    
    // Getters
    
    public Path getLastGeneratedScript() {
        return lastGeneratedScript;
    }
    
    public Path getLastExecutionLog() {
        return lastExecutionLog;
    }
    
    public String getChipsetType() {
        return hardwareLayer.getChipsetType();
    }
}
