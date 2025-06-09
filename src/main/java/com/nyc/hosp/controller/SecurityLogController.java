package com.nyc.hosp.controller;

import com.nyc.hosp.security.SecurityEventLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityLogController {
    
    @Autowired
    private SecurityEventLogger securityEventLogger;
    
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public List<SecurityEventLogger.SecurityEvent> getSecurityLogs() {
        return securityEventLogger.getRecentEvents();
    }
    
    @PostMapping("/logs/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearLogs() {
        securityEventLogger.clearEvents();
        securityEventLogger.logEvent("ADMIN_ACTION", "SYSTEM", "CLEAR_LOGS", "SUCCESS", "Security logs cleared by admin");
        return ResponseEntity.ok().build();
    }
}
