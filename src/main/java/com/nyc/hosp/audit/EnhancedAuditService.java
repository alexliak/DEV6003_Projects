package com.nyc.hosp.audit;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EnhancedAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAuditService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Autowired
    private AuditLogService originalAuditService;
    
    @Async
    public void logDataAccessWithFullDetails(Authentication authentication, String entity, Long entityId, String action, boolean success) {
        try {
            String userDetails = getUserDetailsString(authentication);
            
            // Use enhanced SQL that stores full user details
            String sql = "INSERT INTO audit_log (username, event_type, event_description, entity_type, entity_id, success, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String description = String.format("%s %s with ID %d", action, entity, entityId);
            jdbcTemplate.update(sql, userDetails, "DATA_ACCESS", description, entity, entityId, success, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.info("DATA_ACCESS: {} - {} {} [ID: {}] - Success: {}", userDetails, action, entity, entityId, success);
        } catch (Exception e) {
            logger.error("Failed to log enhanced data access event", e);
            // Fallback to original service
            originalAuditService.logDataAccess(authentication.getName(), entity, entityId, action, success);
        }
    }
    
    @Async
    public void logSecurityViolationWithFullDetails(Authentication authentication, String violationType, String details, String ipAddress) {
        try {
            String userDetails = getUserDetailsString(authentication);
            
            String sql = "INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            String description = String.format("%s: %s", violationType, details);
            jdbcTemplate.update(sql, userDetails, "SECURITY_VIOLATION", description, false, ipAddress, Timestamp.valueOf(LocalDateTime.now()));
            
            logger.error("SECURITY_VIOLATION: {} - {} - IP: {} - Details: {}", userDetails, violationType, ipAddress, details);
        } catch (Exception e) {
            logger.error("Failed to log enhanced security violation", e);
            originalAuditService.logSecurityViolation(authentication.getName(), violationType, details, ipAddress);
        }
    }
    
    @Async
    public void logAuthenticationEventWithFullDetails(Authentication authentication, String event, boolean success, String ipAddress) {
        try {
            String userDetails = getUserDetailsString(authentication);
            
            String sql = "INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, userDetails, "AUTHENTICATION", event, success, ipAddress, Timestamp.valueOf(LocalDateTime.now()));
            
            if (success) {
                logger.info("AUTH_EVENT: {} - {} - Success - IP: {}", userDetails, event, ipAddress);
            } else {
                logger.warn("AUTH_EVENT: {} - {} - Failed - IP: {}", userDetails, event, ipAddress);
            }
        } catch (Exception e) {
            logger.error("Failed to log enhanced authentication event", e);
            originalAuditService.logAuthenticationEvent(authentication.getName(), event, success, ipAddress);
        }
    }
    
    private String getUserDetailsString(Authentication authentication) {
        try {
            if (authentication == null) {
                return "ANONYMOUS|0|NO_ROLE";
            }
            
            // Try to get CustomUserPrincipal
            if (authentication.getPrincipal() instanceof CustomUserPrincipal) {
                CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
                String role = principal.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse("NO_ROLE");
                
                return String.format("%s|%d|%s", principal.getUsername(), principal.getId(), role);
            }
            
            // Try to get from database
            String username = authentication.getName();
            Optional<Hospuser> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                Hospuser user = userOpt.get();
                String role = user.getRoles().stream()
                    .findFirst()
                    .map(r -> r.getName().name().replace("ROLE_", ""))
                    .orElse("NO_ROLE");
                return String.format("%s|%d|%s", username, user.getId(), role);
            }
            
            // Fallback to just username
            return authentication.getName() + "|0|UNKNOWN";
            
        } catch (Exception e) {
            logger.error("Error getting user details for audit", e);
            return authentication != null ? authentication.getName() + "|0|ERROR" : "UNKNOWN|0|ERROR";
        }
    }
}
