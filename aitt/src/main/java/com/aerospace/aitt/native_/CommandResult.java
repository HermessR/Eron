package com.aerospace.aitt.native_;

/**
 * Result of a CMM command execution.
 * 
 * @param success Whether command executed successfully
 * @param response Response data from the command
 * @param errorCode Error code (0 = success)
 * @param errorMessage Error message if failed
 */
public record CommandResult(
    boolean success,
    String response,
    int errorCode,
    String errorMessage
) {
    /**
     * Creates a successful result.
     */
    public static CommandResult success(String response) {
        return new CommandResult(true, response, 0, null);
    }
    
    /**
     * Creates an error result.
     */
    public static CommandResult error(String message) {
        return new CommandResult(false, null, -1, message);
    }
    
    /**
     * Creates an error result with code.
     */
    public static CommandResult error(int code, String message) {
        return new CommandResult(false, null, code, message);
    }
    
    /**
     * Parses result from native response string.
     * Format: "OK:response" or "ERROR:code:message"
     */
    public static CommandResult parse(String raw) {
        if (raw == null) {
            return error("Null response");
        }
        
        if (raw.startsWith("OK:")) {
            return success(raw.substring(3));
        }
        
        if (raw.startsWith("ERROR:")) {
            String[] parts = raw.substring(6).split(":", 2);
            int code = -1;
            String msg = raw.substring(6);
            
            if (parts.length >= 2) {
                try {
                    code = Integer.parseInt(parts[0]);
                    msg = parts[1];
                } catch (NumberFormatException ignored) {
                    // Use full string as message
                }
            }
            
            return error(code, msg);
        }
        
        // Assume success if no prefix
        return success(raw);
    }
    
    /**
     * Gets response or throws if error.
     */
    public String getResponseOrThrow() {
        if (!success) {
            throw new NativeException(errorMessage, errorCode);
        }
        return response;
    }
    
    /**
     * Returns true if response contains the specified string.
     */
    public boolean responseContains(String text) {
        return success && response != null && response.contains(text);
    }
}
