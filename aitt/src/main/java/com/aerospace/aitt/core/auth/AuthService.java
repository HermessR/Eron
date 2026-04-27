package com.aerospace.aitt.core.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication service for user login and session management.
 * In future versions, this will be replaced with license validation.
 */
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;
    
    private User currentUser;
    
    private AuthService() {}
    
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Authenticates a user with username and password.
     * Currently uses simple validation - will be replaced with license system.
     */
    public AuthResult authenticate(String username, String password) {
        log.info("Authentication attempt for user: {}", username);
        
        if (username == null || username.isBlank()) {
            return AuthResult.failure("Username is required");
        }
        
        if (password == null || password.isBlank()) {
            return AuthResult.failure("Password is required");
        }
        
        // Simple authentication for development
        // TODO: Replace with license validation system
        if (isValidCredentials(username, password)) {
            currentUser = new User(
                username,
                formatDisplayName(username),
                determineRole(username),
                LocalDateTime.now()
            );
            log.info("User {} authenticated successfully", username);
            return AuthResult.success(currentUser);
        }
        
        log.warn("Authentication failed for user: {}", username);
        return AuthResult.failure("Invalid username or password");
    }
    
    /**
     * Logs out the current user.
     */
    public void logout() {
        if (currentUser != null) {
            log.info("User {} logged out", currentUser.username());
            currentUser = null;
        }
    }
    
    /**
     * Returns the currently authenticated user.
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }
    
    /**
     * Checks if a user is currently authenticated.
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }
    
    private boolean isValidCredentials(String username, String password) {
        // Development credentials - replace with actual authentication
        // Basic validation: username must be non-empty and password at least 4 chars
        return username != null && !username.isBlank() && password.length() >= 4;
    }
    
    private String formatDisplayName(String username) {
        if (username.contains(".")) {
            String[] parts = username.split("\\.");
            return capitalize(parts[0]) + " " + capitalize(parts[parts.length - 1]);
        }
        return capitalize(username);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private User.UserRole determineRole(String username) {
        if (username.toLowerCase().contains("admin")) {
            return User.UserRole.ADMIN;
        }
        return User.UserRole.ENGINEER;
    }
    
    /**
     * Result of an authentication attempt.
     */
    public record AuthResult(boolean success, User user, String errorMessage) {
        public static AuthResult success(User user) {
            return new AuthResult(true, user, null);
        }
    
        
        public static AuthResult failure(String message) {
            return new AuthResult(false, null, message);
        }
    }
}
