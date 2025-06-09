package com.nyc.hosp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@hospital.com}")
    private String fromEmail;
    
    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;
    
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Reset link: {}", resetLink);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - Hospital Management System");
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #667eea; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                        .button { display: inline-block; padding: 12px 30px; background-color: #667eea; 
                                  color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏥 Hospital Management System</h1>
                        </div>
                        <div class="content">
                            <h2>Password Reset Request</h2>
                            <p>Dear %s,</p>
                            <p>We received a request to reset your password. Click the button below to create a new password:</p>
                            <center>
                                <a href="%s" class="button">Reset Password</a>
                            </center>
                            <p>Or copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; background: #eee; padding: 10px;">%s</p>
                            <p><strong>This link will expire in 24 hours for security reasons.</strong></p>
                            <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Hospital Management System.</p>
                            <p>© 2025 Hospital Management System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, resetLink, resetLink);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String username) {
        logger.info("Attempting to send welcome email to: {} (enabled: {}, mailSender: {})", 
            toEmail, emailEnabled, mailSender != null);
            
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Welcome email would be sent to: {}", toEmail);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Hospital Management System");
            message.setText(String.format("""
                Dear %s,
                
                Welcome to the Hospital Management System!
                
                Your account has been successfully created. You can now log in using your credentials.
                
                For security reasons:
                - Your password expires every 90 days
                - Use a strong password with uppercase, lowercase, numbers, and special characters
                - Your account will be locked after 3 failed login attempts
                
                If you have any questions, please contact the administrator.
                
                Best regards,
                Hospital Management System
                """, username));
            
            logger.info("Sending email from: {} to: {}", fromEmail, toEmail);
            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {} - Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
    
    public void sendTemporaryPasswordEmail(String toEmail, String username, String temporaryPassword) {
        if (!emailEnabled || mailSender == null) {
            logger.warn("Email service not configured. Temporary password: {}", temporaryPassword);
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset by Administrator - Hospital Management System");
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f59e0b; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                        .warning { background-color: #fee; padding: 15px; border-left: 4px solid #f44336; margin: 15px 0; }
                        .password-box { background: #e3f2fd; padding: 15px; border-radius: 5px; 
                                       font-family: monospace; font-size: 18px; text-align: center; margin: 20px 0; }
                        .footer { margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔒 Password Reset by Administrator</h1>
                        </div>
                        <div class="content">
                            <h2>Your Password Has Been Reset</h2>
                            <p>Dear %s,</p>
                            <p>An administrator has reset your password. Your temporary password is:</p>
                            <div class="password-box">
                                %s
                            </div>
                            <div class="warning">
                                <strong>⚠️ Important Security Notice:</strong>
                                <ul>
                                    <li>You will be required to change this password immediately upon login</li>
                                    <li>This temporary password is only valid for your first login</li>
                                    <li>For security reasons, create a strong password that meets all requirements</li>
                                </ul>
                            </div>
                            <p><strong>Password Requirements:</strong></p>
                            <ul>
                                <li>At least 8 characters long</li>
                                <li>Contains at least one uppercase letter (A-Z)</li>
                                <li>Contains at least one lowercase letter (a-z)</li>
                                <li>Contains at least one number (0-9)</li>
                                <li>Contains at least one special character (!@#$%%^&*)</li>
                            </ul>
                            <p>If you did not expect this password reset, please contact your administrator immediately.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated security message from the Hospital Management System.</p>
                            <p>© 2025 Hospital Management System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, temporaryPassword);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Temporary password email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send temporary password email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }
}
