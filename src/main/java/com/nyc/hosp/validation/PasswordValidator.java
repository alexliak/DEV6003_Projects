package com.nyc.hosp.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final int PASSWORD_HISTORY_SIZE = 5; // Remember last 5 passwords
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ValidationResult validate(String password) {
        ValidationResult result = new ValidationResult();
        
        if (password == null || password.isEmpty()) {
            result.addError("Password cannot be empty");
            return result;
        }
        
        if (password.length() < MIN_LENGTH) {
            result.addError("Password must be at least " + MIN_LENGTH + " characters");
        }
        
        if (password.length() > MAX_LENGTH) {
            result.addError("Password must be no more than " + MAX_LENGTH + " characters");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            result.addError("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            result.addError("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[0-9].*")) {
            result.addError("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            result.addError("Password must contain at least one special character");
        }
        
        return result;
    }
    
    public boolean isPasswordInHistory(String newPassword, String passwordHistoryJson) {
        if (passwordHistoryJson == null || passwordHistoryJson.isEmpty()) {
            return false;
        }
        
        try {
            List<String> passwordHistory = objectMapper.readValue(
                passwordHistoryJson, 
                new TypeReference<List<String>>() {}
            );
            
            for (String oldHashedPassword : passwordHistory) {
                if (passwordEncoder.matches(newPassword, oldHashedPassword)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If parsing fails, assume no history
            return false;
        }
        
        return false;
    }
    
    public String updatePasswordHistory(String currentHashedPassword, String passwordHistoryJson) {
        List<String> passwordHistory = new ArrayList<>();
        
        if (passwordHistoryJson != null && !passwordHistoryJson.isEmpty()) {
            try {
                passwordHistory = objectMapper.readValue(
                    passwordHistoryJson, 
                    new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                // Start fresh if parsing fails
            }
        }
        
        // Add new password at the beginning
        passwordHistory.add(0, currentHashedPassword);
        
        // Keep only the last N passwords
        if (passwordHistory.size() > PASSWORD_HISTORY_SIZE) {
            passwordHistory = passwordHistory.subList(0, PASSWORD_HISTORY_SIZE);
        }
        
        try {
            return objectMapper.writeValueAsString(passwordHistory);
        } catch (Exception e) {
            return "[]";
        }
    }
    
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
