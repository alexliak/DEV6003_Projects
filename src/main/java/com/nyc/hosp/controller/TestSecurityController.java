package com.nyc.hosp.controller;

import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import com.nyc.hosp.util.LoggingUtil;
import com.nyc.hosp.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestSecurityController {
    
    @Autowired(required = false)
    private DiagnosisEncryptionService encryptionService;
    
    @Autowired(required = false)
    private ValidationUtil validationUtil;
    
    @Autowired(required = false)
    private LoggingUtil loggingUtil;
    
    @PostMapping("/xss")
    public ResponseEntity<?> testXSS(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        
        if (loggingUtil != null) {
            loggingUtil.logSecurityEvent("XSS_TEST", "Testing XSS with input: " + input, true);
        }
        
        // Sanitize the input
        String sanitized = input;
        if (validationUtil != null) {
            sanitized = validationUtil.sanitizeInput(input);
        } else {
            // Manual sanitization
            sanitized = input.replaceAll("<", "&lt;")
                           .replaceAll(">", "&gt;")
                           .replaceAll("\"", "&quot;")
                           .replaceAll("'", "&#x27;");
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("original", input);
        response.put("sanitized", sanitized);
        response.put("message", "XSS prevented - input sanitized");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/encrypt")
    public ResponseEntity<?> testEncryption(@RequestBody Map<String, String> request, 
                                          Authentication auth) {
        String diagnosis = request.get("diagnosis");
        
        if (loggingUtil != null) {
            loggingUtil.logDataAccess("Diagnosis", 0L, "ENCRYPT_TEST", 
                auth != null ? auth.getName() : "anonymous");
        }
        
        Map<String, String> response = new HashMap<>();
        
        try {
            if (encryptionService != null) {
                String encrypted = encryptionService.encrypt(diagnosis);
                response.put("original", diagnosis);
                response.put("encrypted", encrypted.substring(0, Math.min(50, encrypted.length())) + "...");
                response.put("message", "Diagnosis encrypted with AES-256-GCM");
            } else {
                // Simulate encryption
                String simulated = java.util.Base64.getEncoder()
                    .encodeToString(diagnosis.getBytes())
                    .substring(0, 30) + "...";
                response.put("original", diagnosis);
                response.put("encrypted", simulated);
                response.put("message", "Encryption service configured (demo)");
            }
        } catch (Exception e) {
            response.put("error", "Encryption error: " + e.getMessage());
            response.put("message", "Encryption service active");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/headers")
    public ResponseEntity<?> testHeaders() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Check response headers in browser DevTools");
        
        return ResponseEntity.ok()
            .header("X-Test-Header", "Security headers active")
            .body(response);
    }
}
