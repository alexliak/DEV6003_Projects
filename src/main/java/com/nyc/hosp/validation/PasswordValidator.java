package com.nyc.hosp.validation;

import org.passay.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    
    private final org.passay.PasswordValidator validator;
    
    public PasswordValidator() {
        List<Rule> rules = new ArrayList<>();
        
        // Length rule
        rules.add(new LengthRule(MIN_LENGTH, MAX_LENGTH));
        
        // At least one uppercase letter
        rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        
        // At least one lowercase letter
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        
        // At least one digit
        rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        
        // At least one special character
        rules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        
        // No whitespace
        rules.add(new WhitespaceRule());
        
        // No common passwords
        List<String> commonPasswords = List.of(
            "password", "Password", "Password1", "Password123",
            "12345678", "87654321", "qwerty", "abc123",
            "monkey", "1234567890", "letmein", "dragon",
            "111111", "baseball", "iloveyou", "trustno1",
            "sunshine", "master", "123456789", "welcome",
            "shadow", "ashley", "football", "jesus",
            "michael", "ninja", "mustang", "password1"
        );
        rules.add(new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false));
        rules.add(new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false));
        
        this.validator = new org.passay.PasswordValidator(rules);
    }
    
    public ValidationResult validate(String password) {
        RuleResult result = validator.validate(new PasswordData(password));
        
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(result.isValid());
        
        if (!result.isValid()) {
            List<String> messages = validator.getMessages(result);
            validationResult.setErrors(messages);
        }
        
        return validationResult;
    }
    
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
    }
}
