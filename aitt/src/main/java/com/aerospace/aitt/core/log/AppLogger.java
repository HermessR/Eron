package com.aerospace.aitt.core.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized application logger that supports both file logging
 * and UI log panel updates via listeners.
 */
public class AppLogger {
    
    private static final Logger fileLogger = LoggerFactory.getLogger(AppLogger.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final List<Consumer<LogEntry>> listeners = new CopyOnWriteArrayList<>();
    private static final List<LogEntry> buffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000;
    
    private final String context;
    
    public AppLogger(String context) {
        this.context = context;
    }
    
    public AppLogger(Class<?> clazz) {
        this.context = clazz.getSimpleName();
    }
    
    /**
     * Logs an info message.
     */
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    /**
     * Logs an info message with format arguments.
     */
    public void info(String format, Object... args) {
        log(LogLevel.INFO, String.format(format, args));
    }
    
    /**
     * Logs a warning message.
     */
    public void warn(String message) {
        log(LogLevel.WARN, message);
    }
    
    /**
     * Logs a warning message with format arguments.
     */
    public void warn(String format, Object... args) {
        log(LogLevel.WARN, String.format(format, args));
    }
    
    /**
     * Logs an error message.
     */
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    /**
     * Logs an error message with format arguments.
     */
    public void error(String format, Object... args) {
        log(LogLevel.ERROR, String.format(format, args));
    }
    
    /**
     * Logs an error message with exception.
     */
    public void error(String message, Throwable t) {
        log(LogLevel.ERROR, message + ": " + t.getMessage());
        fileLogger.error(message, t);
    }
    
    /**
     * Logs a debug message.
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    /**
     * Logs a debug message with format arguments.
     */
    public void debug(String format, Object... args) {
        log(LogLevel.DEBUG, String.format(format, args));
    }
    
    private void log(LogLevel level, String message) {
        LogEntry entry = new LogEntry(
            LocalDateTime.now(),
            level,
            context,
            message
        );
        
        // Log to file
        switch (level) {
            case DEBUG -> fileLogger.debug("[{}] {}", context, message);
            case INFO -> fileLogger.info("[{}] {}", context, message);
            case WARN -> fileLogger.warn("[{}] {}", context, message);
            case ERROR -> fileLogger.error("[{}] {}", context, message);
        }
        
        // Add to buffer
        synchronized (buffer) {
            buffer.add(entry);
            if (buffer.size() > MAX_BUFFER_SIZE) {
                buffer.remove(0);
            }
        }
        
        // Notify listeners
        for (Consumer<LogEntry> listener : listeners) {
            try {
                listener.accept(entry);
            } catch (Exception e) {
                fileLogger.error("Failed to notify log listener", e);
            }
        }
    }
    
    /**
     * Adds a listener for log entries.
     */
    public static void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }
    
    /**
     * Sets a UI string listener that receives formatted log messages.
     */
    public static void setUiListener(Consumer<String> uiListener) {
        addListener(entry -> uiListener.accept(entry.formatted()));
    }
    
    /**
     * Removes a log listener.
     */
    public static void removeListener(Consumer<LogEntry> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Returns the buffered log entries.
     */
    public static List<LogEntry> getBuffer() {
        synchronized (buffer) {
            return new ArrayList<>(buffer);
        }
    }
    
    /**
     * Clears all log listeners.
     */
    public static void clearListeners() {
        listeners.clear();
    }
    
    /**
     * Log entry record.
     */
    public record LogEntry(
        LocalDateTime timestamp,
        LogLevel level,
        String context,
        String message
    ) {
        public String formatted() {
            return String.format("[%s] %-5s %s",
                timestamp.format(TIME_FORMAT),
                level,
                message
            );
        }
    }
    
    /**
     * Log levels.
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
