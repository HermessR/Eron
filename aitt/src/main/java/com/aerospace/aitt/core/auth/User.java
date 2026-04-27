package com.aerospace.aitt.core.auth;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an authenticated user in the system.
 */
public record User(
    String username,
    String displayName,
    UserRole role,
    LocalDateTime loginTime
) {
    public User {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        if (displayName == null || displayName.isBlank()) {
            displayName = username;
        }
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
    
    public enum UserRole {
        OPERATOR,
        ENGINEER,
        ADMIN
    }
}
