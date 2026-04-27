package com.aerospace.aitt.native_;

/**
 * Exception thrown by native code operations.
 */
public class NativeException extends RuntimeException {
    
    private final int errorCode;
    
    public NativeException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    public NativeException(String message, int errorCode) {
        super(message + " (code: " + errorCode + ")");
        this.errorCode = errorCode;
    }
    
    public NativeException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
