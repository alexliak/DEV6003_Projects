package com.nyc.hosp.security;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SecurityEventLogger {
    
    private static final int MAX_EVENTS = 100;
    private final List<SecurityEvent> events = Collections.synchronizedList(new ArrayList<>());
    
    public static class SecurityEvent {
        private final LocalDateTime timestamp;
        private final String type;
        private final String user;
        private final String action;
        private final String result;
        private final String details;
        
        public SecurityEvent(String type, String user, String action, String result, String details) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.user = user;
            this.action = action;
            this.result = result;
            this.details = details;
        }
        
        // Getters
        public String getTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        public String getType() { return type; }
        public String getUser() { return user; }
        public String getAction() { return action; }
        public String getResult() { return result; }
        public String getDetails() { return details; }
    }
    
    public void logEvent(String type, String user, String action, String result, String details) {
        SecurityEvent event = new SecurityEvent(type, user, action, result, details);
        events.add(0, event); // Add to beginning
        
        // Keep only last MAX_EVENTS
        if (events.size() > MAX_EVENTS) {
            events.remove(events.size() - 1);
        }
        
        // Also log to console for demonstration
        System.out.println(String.format(
            "[SECURITY-LOG] %s | Type: %s | User: %s | Action: %s | Result: %s | Details: %s",
            event.getTimestamp(), type, user, action, result, details
        ));
    }
    
    public List<SecurityEvent> getRecentEvents() {
        return new ArrayList<>(events);
    }
    
    public void clearEvents() {
        events.clear();
    }
}
