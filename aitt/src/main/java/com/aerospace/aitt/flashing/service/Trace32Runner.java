package com.aerospace.aitt.flashing.service;

import com.aerospace.aitt.core.log.AppLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Executes TRACE32 with generated CMM scripts.
 * Manages process lifecycle and output capture.
 */
public class Trace32Runner {
    
    private final AppLogger log = new AppLogger(Trace32Runner.class);
    
    private static final Path DEFAULT_T32_PATH = Path.of("C:/T32/bin/windows64/t32marm.exe");
    private static final long DEFAULT_TIMEOUT_MINUTES = 30;
    
    private volatile Process currentProcess;
    private volatile boolean killed = false;
    
    /**
     * Finds the TRACE32 executable path.
     * Checks environment variable T32SYS, then default installation path.
     */
    public static Path findTrace32Executable() {
        // Check T32SYS environment variable
        String t32sys = System.getenv("T32SYS");
        if (t32sys != null) {
            Path envPath = Path.of(t32sys, "bin", "windows64", "t32marm.exe");
            if (Files.exists(envPath)) {
                return envPath;
            }
        }
        
        // Check default path
        if (Files.exists(DEFAULT_T32_PATH)) {
            return DEFAULT_T32_PATH;
        }
        
        // Return default even if not found (will error later)
        return DEFAULT_T32_PATH;
    }
    
    /**
     * Runs TRACE32 with the specified configuration and script.
     *
     * @param t32Executable Path to TRACE32 executable
     * @param configPath Path to .t32 configuration file
     * @param scriptPath Path to CMM script
     * @param outputCallback Callback for process output
     * @return Process exit code
     */
    public int run(Path t32Executable, Path configPath, Path scriptPath, 
                   Consumer<String> outputCallback) throws IOException, InterruptedException {
        
        killed = false;
        
        // Resolve executable path
        Path executable = t32Executable != null ? t32Executable : DEFAULT_T32_PATH;
        
        // Validate paths
        if (!Files.exists(executable)) {
            throw new IOException("TRACE32 executable not found: " + executable);
        }
        
        if (!Files.exists(scriptPath)) {
            throw new IOException("CMM script not found: " + scriptPath);
        }
        
        // Build command
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        
        if (configPath != null && Files.exists(configPath)) {
            command.add("-c");
            command.add(configPath.toString());
        }
        
        command.add("-s");
        command.add(scriptPath.toString());
        
        log.info("Launching TRACE32: %s", String.join(" ", command));
        outputCallback.accept("Launching: " + executable.getFileName());
        
        // Start process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        currentProcess = pb.start();
        
        // Read output in separate thread
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!killed) {
                        outputCallback.accept(line);
                    }
                }
            } catch (IOException e) {
                if (!killed) {
                    log.error("Error reading TRACE32 output", e);
                }
            }
        }, "t32-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();
        
        // Wait for completion with timeout
        boolean completed = currentProcess.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        
        if (!completed) {
            log.warn("TRACE32 timed out after %d minutes", DEFAULT_TIMEOUT_MINUTES);
            currentProcess.destroyForcibly();
            outputCallback.accept("ERROR: TRACE32 timed out");
            return -1;
        }
        
        int exitCode = currentProcess.exitValue();
        currentProcess = null;
        
        if (killed) {
            log.info("TRACE32 was killed by user");
            return -2;
        }
        
        log.info("TRACE32 exited with code: %d", exitCode);
        return exitCode;
    }
    
    /**
     * Kills the running TRACE32 process.
     */
    public void kill() {
        killed = true;
        if (currentProcess != null && currentProcess.isAlive()) {
            log.info("Killing TRACE32 process");
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }
    
    /**
     * Checks if TRACE32 is currently running.
     */
    public boolean isRunning() {
        return currentProcess != null && currentProcess.isAlive();
    }
    
    /**
     * Finds TRACE32 installation path.
     */
    public static Path findTrace32() {
        // Build search paths with null-safe environment variable access
        List<Path> searchPaths = new ArrayList<>();
        searchPaths.add(Path.of("C:/T32/bin/windows64/t32marm.exe"));
        searchPaths.add(Path.of("C:/T32/bin/windows/t32marm.exe"));
        searchPaths.add(Path.of("D:/T32/bin/windows64/t32marm.exe"));
        
        String programFiles = System.getenv("PROGRAMFILES");
        if (programFiles != null) {
            searchPaths.add(Path.of(programFiles, "T32/bin/windows64/t32marm.exe"));
        }
        
        String programFilesX86 = System.getenv("PROGRAMFILES(X86)");
        if (programFilesX86 != null) {
            searchPaths.add(Path.of(programFilesX86, "T32/bin/windows/t32marm.exe"));
        }
        
        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        
        // Check PATH environment with platform-safe separator
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
                Path t32Path = Path.of(dir, "t32marm.exe");
                if (Files.exists(t32Path)) {
                    return t32Path;
                }
            }
        }
        
        return DEFAULT_T32_PATH;
    }
    
    /**
     * Validates that TRACE32 is installed and accessible.
     */
    public static boolean isTrace32Available() {
        Path t32Path = findTrace32();
        return Files.exists(t32Path) && Files.isExecutable(t32Path);
    }
}
