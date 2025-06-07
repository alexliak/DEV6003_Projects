package com.nyc.hosp.validation;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {
    
    private static final Logger logger = LoggerFactory.getLogger(InputSanitizer.class);
    
    // SQL Injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|alert|onload|onerror|onclick).*"
    );
    
    // XSS patterns
    private static final Pattern XSS_PATTERN = Pattern.compile(
        ".*(<script|</script|<iframe|</iframe|javascript:|onerror=|onload=|onclick=|<img|<svg).*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Special characters that might be used for injection
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(
        ".*[';\"\\-\\-\\/\\*\\*\\/].*"
    );
    
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).matches()) {
            logger.warn("SQL injection pattern detected in input");
            return false;
        }
        
        // Check for XSS patterns
        if (XSS_PATTERN.matcher(input).matches()) {
            logger.warn("XSS pattern detected in input");
            return false;
        }
        
        return true;
    }
    
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potential XSS
        String sanitized = input
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#x27;")
            .replaceAll("/", "&#x2F;");
        
        // Remove any script tags or javascript
        sanitized = sanitized.replaceAll("(?i)<script.*?>.*?</script>", "");
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        
        return sanitized.trim();
    }
    
    public boolean containsSQLInjection(String input) {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    public boolean containsXSS(String input) {
        return input != null && XSS_PATTERN.matcher(input).matches();
    }
}
