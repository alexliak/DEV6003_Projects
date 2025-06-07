package com.nyc.hosp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Async
    public void logAuthenticationEvent(String username, String event, boolean success, String ipAddress) {
        try {
            String sql = "INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, username, "AUTHENTICATION", event, success, ipAddress, Timestamp.valueOf(LocalDateTime.now()));
            
            if (success) {
                logger.info("AUTH_EVENT: User {} - {} - Success - IP: {}", username, event, ipAddress);
            } else {
                logger.warn("AUTH_EVENT: User {} - {} - Failed - IP: {}", username, event, ipAddress);
            }
        } catch (Exception e) {
            logger.error("Failed to log authentication event", e);
        }
    }
    
    @Async
    public void logDataAccess(String username, String entity, Long entityId, String action, boolean success) {
        try {
            String sql = "INSERT INTO audit_log (username, event_type, event_description, entity_type, entity_id, success, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String description = String.format("%s %s with ID %d", action, entity, entityId);
            jdbcTemplate.update(sql, username, "DATA_ACCESS", description, entity, entityId, success, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.info("DATA_ACCESS: User {} - {} {} [ID: {}] - Success: {}", username, action, entity, entityId, success);
        } catch (Exception e) {
            logger.error("Failed to log data access event", e);
        }
    }
    
    @Async
    public void logSecurityViolation(String username, String violationType, String details, String ipAddress) {
        try {
            String sql = "INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            String description = String.format("%s: %s", violationType, details);
            jdbcTemplate.update(sql, username, "SECURITY_VIOLATION", description, false, ipAddress, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.error("SECURITY_VIOLATION: User {} - {} - IP: {} - Details: {}", username, violationType, ipAddress, details);
        } catch (Exception e) {
            logger.error("Failed to log security violation", e);
        }
    }
    
    @Async
    public void logPasswordChange(String username, boolean success) {
        try {
            String sql = "INSERT INTO audit_log (username, event_type, event_description, success, timestamp) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, username, "PASSWORD_CHANGE", "Password changed", success, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.info("PASSWORD_CHANGE: User {} - Success: {}", username, success);
        } catch (Exception e) {
            logger.error("Failed to log password change event", e);
        }
    }
    
    @Async
    public void logAdminAction(String adminUsername, String action, String targetUser, String details) {
        try {
            String sql = "INSERT INTO audit_log (username, event_type, event_description, target_user, success, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            String description = String.format("%s: %s", action, details);
            jdbcTemplate.update(sql, adminUsername, "ADMIN_ACTION", description, targetUser, true, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.info("ADMIN_ACTION: Admin {} - {} - Target: {} - Details: {}", adminUsername, action, targetUser, details);
        } catch (Exception e) {
            logger.error("Failed to log admin action", e);
        }
    }
}
