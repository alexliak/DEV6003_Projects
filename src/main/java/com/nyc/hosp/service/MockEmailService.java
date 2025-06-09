package com.nyc.hosp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Mock Email Service for demonstration purposes
 * Stores emails in memory instead of sending them
 */
@Service
public class MockEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    
    // In-memory storage for demo emails
    private final List<MockEmail> sentEmails = new ArrayList<>();
    
    public static class MockEmail {
        private String to;
        private String subject;
        private String content;
        private LocalDateTime sentAt;
        private String type; // WELCOME, PASSWORD_RESET, etc.
        
        public MockEmail(String to, String subject, String content, String type) {
            this.to = to;
            this.subject = subject;
            this.content = content;
            this.type = type;
            this.sentAt = LocalDateTime.now();
        }
        
        // Getters
        public String getTo() { return to; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public String getType() { return type; }
        public LocalDateTime getSentAt() { return sentAt; }
    }
    
    public String sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        String content = String.format("""
            Dear %s,
            
            You requested a password reset. Click the link below:
            %s
            
            This link expires in 24 hours.
            
            If you didn't request this, ignore this email.
            """, username, resetLink);
        
        MockEmail email = new MockEmail(toEmail, "Password Reset", content, "PASSWORD_RESET");
        sentEmails.add(email);
        
        logger.info("Mock email sent to: {} - Reset link: {}", toEmail, resetLink);
        
        // Return the reset link for demo display
        return resetLink;
    }
    
    public void sendWelcomeEmail(String toEmail, String username) {
        String content = String.format("""
            Welcome %s!
            
            Your account has been created successfully.
            
            Security reminders:
            - Password expires every 90 days
            - Account locks after 3 failed attempts
            - Use strong passwords
            """, username);
        
        MockEmail email = new MockEmail(toEmail, "Welcome to Hospital System", content, "WELCOME");
        sentEmails.add(email);
        
        logger.info("Mock welcome email sent to: {}", toEmail);
    }
    
    public List<MockEmail> getAllSentEmails() {
        return new ArrayList<>(sentEmails);
    }
    
    public List<MockEmail> getEmailsForUser(String email) {
        return sentEmails.stream()
            .filter(e -> e.getTo().equals(email))
            .toList();
    }
    
    public void clearAllEmails() {
        sentEmails.clear();
    }
}
