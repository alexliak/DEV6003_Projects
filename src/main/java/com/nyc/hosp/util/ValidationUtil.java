package com.nyc.hosp.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class ValidationUtil {
    
    // Patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        ".*([';\"\\-\\-\\/\\*\\*\\/]+|\\b(OR|AND|UNION|SELECT|INSERT|UPDATE|DELETE|DROP|CREATE)\\b).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*(<script>|<\\/script>|javascript:|onerror=|onclick=|onload=|<iframe|<object|<embed).*",
        Pattern.CASE_INSENSITIVE
    );
    
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public boolean containsSQLInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    public boolean containsXSS(String input) {
        if (input == null) return false;
        return XSS_PATTERN.matcher(input).matches();
    }
    
    public String sanitizeInput(String input) {
        if (input == null) return null;
        
        // HTML encode special characters
        String sanitized = input
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#x27;")
            .replaceAll("/", "&#x2F;");
        
        return sanitized.trim();
    }
    
    public boolean validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // Check for at least one digit
        if (!password.matches(".*[0-9].*")) {
            return false;
        }
        
        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()].*")) {
            return false;
        }
        
        return true;
    }
    
    public String getPasswordValidationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        
        StringBuilder message = new StringBuilder();
        
        if (password.length() < 8) {
            message.append("Password must be at least 8 characters long. ");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            message.append("Password must contain at least one uppercase letter. ");
        }
        
        if (!password.matches(".*[a-z].*")) {
            message.append("Password must contain at least one lowercase letter. ");
        }
        
        if (!password.matches(".*[0-9].*")) {
            message.append("Password must contain at least one digit. ");
        }
        
        if (!password.matches(".*[!@#$%^&*()].*")) {
            message.append("Password must contain at least one special character (!@#$%^&*()). ");
        }
        
        return message.length() > 0 ? message.toString().trim() : "Password is valid";
    }
}
