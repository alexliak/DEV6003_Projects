package com.nyc.hosp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LoggingUtil {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingUtil.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void logSecurityEvent(String eventType, String message, boolean success) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logLevel = success ? "INFO" : "WARN";
        String logMessage = String.format("[SECURITY-LOG] %s | Type: %s | Success: %s | Details: %s", 
            timestamp, eventType, success, message);
        
        if (success) {
            log.info(logMessage);
        } else {
            log.warn(logMessage);
        }
        
        // Also print to console for demo visibility
        System.out.println(logMessage);
    }
    
    public void logDataAccess(String entity, Long entityId, String action, String username) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[DATA-ACCESS-LOG] %s | User: %s | Action: %s | Entity: %s | ID: %s", 
            timestamp, username, action, entity, entityId);
        
        log.info(logMessage);
        System.out.println(logMessage);
    }
    
    public void logAuthenticationAttempt(String username, boolean success, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String eventType = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        String logMessage = String.format("[AUTH-LOG] %s | Type: %s | User: %s | Details: %s", 
            timestamp, eventType, username, details);
        
        if (success) {
            log.info(logMessage);
        } else {
            log.warn(logMessage);
        }
        
        System.out.println(logMessage);
    }
    
    public void logPasswordChange(String username, boolean success) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[PASSWORD-LOG] %s | User: %s | Password Change: %s", 
            timestamp, username, success ? "SUCCESS" : "FAILED");
        
        log.info(logMessage);
        System.out.println(logMessage);
    }
}
