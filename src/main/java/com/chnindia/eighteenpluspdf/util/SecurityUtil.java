package com.chnindia.eighteenpluspdf.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecurityUtil {
    
    @Value("${app.security.api-key-enabled:true}")
    private boolean apiKeyEnabled;
    
    @Value("${app.security.jwt-enabled:true}")
    private boolean jwtEnabled;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int API_KEY_LENGTH = 32;
    
    /**
     * Generate a secure random API key
     */
    public String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Get current authenticated user
     */
    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
    
    /**
     * Check if user has API access
     */
    public boolean hasApiAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               (authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_API") || a.getAuthority().equals("ROLE_USER")));
    }
    
    /**
     * Validate input for path traversal
     */
    public String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Check if string is safe for file operations
     */
    public boolean isSafePath(String path) {
        if (path == null) return false;
        return !path.contains("..") && !path.contains("/") && !path.contains("\\");
    }
    
    /**
     * Mask sensitive information
     */
    public String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) return "****";
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
}