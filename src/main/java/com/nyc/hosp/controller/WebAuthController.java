package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.dto.PasswordResetRequest;
import com.nyc.hosp.model.PasswordResetToken;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PasswordResetTokenRepository;
import com.nyc.hosp.repos.RoleRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.service.UserService;
import com.nyc.hosp.service.EmailService;
import com.nyc.hosp.util.LoggingUtil;
import com.nyc.hosp.validation.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class WebAuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private LoggingUtil loggingUtil;
    
    @GetMapping("/login")
    public String showLoginForm() {
        // Use the existing login.html for now
        return "auth/login";
    }
    
    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        return "auth/change-password";
    }
    
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            // Check if passwords match
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "New passwords do not match");
                return "auth/change-password";
            }
            
            // Validate password complexity
            var validationResult = passwordValidator.validate(newPassword);
            if (validationResult.hasErrors()) {
                model.addAttribute("error", String.join(". ", validationResult.getErrors()));
                return "auth/change-password";
            }
            
            // Get the principal
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // Change password
            boolean success = userService.changePassword(
                principal.getUsername(), 
                oldPassword, 
                newPassword
            );
            
            if (success) {
                redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                    "Password changed successfully! Please login with your new password.");
                
                // Force re-login with new password
                SecurityContextHolder.clearContext();
                return "redirect:/auth/login";
            } else {
                // Check WHY it failed
                Hospuser user = userRepository.findByUsername(principal.getUsername()).orElse(null);
                if (user != null) {
                    // Check if wrong old password
                    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                        model.addAttribute("error", "Current password is incorrect");
                    }
                    // Check if trying to reuse current password
                    else if (passwordEncoder.matches(newPassword, user.getPassword())) {
                        model.addAttribute("error", "New password must be different from current password");
                    }
                    // Check if password in history
                    else if (passwordValidator.isPasswordInHistory(newPassword, user.getPasswordHistory())) {
                        model.addAttribute("error", "This password has been used recently. Please choose a different password.");
                    } else {
                        model.addAttribute("error", "Password change failed");
                    }
                } else {
                    model.addAttribute("error", "User not found");
                }
                return "auth/change-password";
            }
            
        } catch (Exception e) {
            model.addAttribute("error", "Error changing password: " + e.getMessage());
            return "auth/change-password";
        }
    }
    
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "auth/register";
    }
    
    @PostMapping("/register")
    @Transactional
    public String processRegistration(@RequestParam String username,
                                    @RequestParam String email,
                                    @RequestParam String firstName,
                                    @RequestParam String lastName,
                                    @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfBirth,
                                    @RequestParam String password,
                                    @RequestParam String confirmPassword,
                                    @RequestParam Long role,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Check if passwords match
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match");
                return "auth/register";
            }
            
            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                model.addAttribute("error", "Username already exists");
                return "auth/register";
            }
            
            // Check if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Email already exists");
                return "auth/register";
            }
            
            // Validate password complexity
            var validationResult = passwordValidator.validate(password);
            if (validationResult.hasErrors()) {
                model.addAttribute("error", String.join(". ", validationResult.getErrors()));
                return "auth/register";
            }
            
            // Create new user
            Hospuser newUser = new Hospuser();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setDateOfBirth(dateOfBirth);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setLastPasswordChange(new Date());
            newUser.setAccountLocked(false);
            newUser.setFailedLoginAttempts(0);
            newUser.setForcePasswordChange(false);
            
            // Set role
            com.nyc.hosp.domain.Role userRole = roleRepository.findById(role)
                .orElseThrow(() -> new RuntimeException("Role not found"));
            newUser.getRoles().add(userRole);
            
            // Save user
            userRepository.save(newUser);
            
            // Log the event
            loggingUtil.logSecurityEvent("USER_REGISTERED", username, true);
            
            // Send welcome email via Mailtrap
            emailService.sendWelcomeEmail(email, username);
            
            redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                "Registration successful! Welcome email sent. Please check your email and login.");
            
            return "redirect:/auth/login";
            
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }
    
    @GetMapping("/account-locked")
    public String showAccountLocked() {
        return "auth/account-locked";
    }
    
    @PostMapping("/request-unlock")
    public String requestUnlock(@RequestParam String email, Model model) {
        // For demo purposes, generate unlock token
        String unlockToken = UUID.randomUUID().toString();
        String unlockLink = "https://localhost:8443/auth/unlock?token=" + unlockToken + "&email=" + email;
        
        // In production, this would send email
        model.addAttribute("MSG_SUCCESS", "Unlock link (for demo): " + unlockLink);
        model.addAttribute("unlockToken", unlockToken);
        
        return "auth/unlock-sent";
    }
    
    // ==================== FORGOT PASSWORD FUNCTIONALITY ====================
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    @Transactional
    public String processForgotPassword(@RequestParam String email, 
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            // Find user by email
            Hospuser user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // Don't reveal if email exists or not for security
                redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                    "If the email exists in our system, you will receive a password reset link.");
                return "redirect:/auth/forgot-password";
            }
            
            // Invalidate any existing tokens
            List<PasswordResetToken> existingTokens = tokenRepository
                .findByUserAndUsedFalseAndExpiryDateGreaterThan(user, LocalDateTime.now());
            existingTokens.forEach(token -> token.setUsed(true));
            tokenRepository.saveAll(existingTokens);
            
            // Generate new token
            String resetToken = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = new PasswordResetToken(resetToken, user);
            tokenRepository.save(passwordResetToken);
            
            // Generate reset link
            String resetLink = "https://localhost:8443/auth/reset-password?token=" + resetToken;
            
            // Send real email via Mailtrap
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
            
            // Log the event
            loggingUtil.logSecurityEvent("PASSWORD_RESET_REQUESTED", user.getUsername(), true);
            
            redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                "Password reset instructions have been sent to your email address.");
            
            return "redirect:/auth/forgot-password";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error processing request. Please try again.");
            return "auth/forgot-password";
        }
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String token, 
                                      Model model) {
        if (token == null || token.isEmpty()) {
            model.addAttribute("error", "Invalid reset link");
            return "auth/reset-password";
        }
        
        // Validate token
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);
        
        if (resetToken == null || resetToken.isUsed() || resetToken.isExpired()) {
            model.addAttribute("error", "This reset link is invalid or has expired");
            return "auth/reset-password";
        }
        
        model.addAttribute("token", token);
        model.addAttribute("email", resetToken.getUser().getEmail());
        return "auth/reset-password";
    }
    
    @PostMapping("/reset-password")
    @Transactional
    public String processResetPassword(@RequestParam String token,
                                     @RequestParam String newPassword,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        try {
            // Validate token
            PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);
            
            if (resetToken == null || resetToken.isUsed() || resetToken.isExpired()) {
                redirectAttributes.addFlashAttribute("error", 
                    "This reset link is invalid or has expired");
                return "redirect:/auth/forgot-password";
            }
            
            // Check if passwords match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/auth/reset-password?token=" + token;
            }
            
            // Validate password complexity
            var validationResult = passwordValidator.validate(newPassword);
            if (validationResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error", 
                    String.join(". ", validationResult.getErrors()));
                return "redirect:/auth/reset-password?token=" + token;
            }
            
            // Check password history - ΠΡΟΣΘΗΚΗ
            Hospuser user = resetToken.getUser();
            
            // Check if trying to reuse current password
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your new password must be different from your current password.");
                return "redirect:/auth/reset-password?token=" + token;
            }
            
            // Check password history (last 5 passwords)
            if (passwordValidator.isPasswordInHistory(newPassword, user.getPasswordHistory())) {
                redirectAttributes.addFlashAttribute("error", 
                    "This password has been used recently. You cannot reuse any of your last 5 passwords. Please choose a different password.");
                return "redirect:/auth/reset-password?token=" + token;
            }
            
            // Update password and history
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHistory(passwordValidator.updatePasswordHistory(hashedPassword, user.getPasswordHistory()));
            user.setPassword(hashedPassword);
            user.setLastPasswordChange(new Date());
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            userRepository.save(user);
            
            // Mark token as used
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            
            // Log the event
            loggingUtil.logSecurityEvent("PASSWORD_RESET_COMPLETED", user.getUsername(), true);
            
            redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                "Password reset successfully! Please login with your new password.");
            
            return "redirect:/auth/login";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error resetting password: " + e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }
}
