package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.validation.PasswordValidator;
import com.nyc.hosp.util.ValidationUtil;
import com.nyc.hosp.audit.AuditLogService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import com.nyc.hosp.util.LoggingUtil;

import java.util.Date;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private ValidationUtil validationUtil;
    
    @Autowired
    private LoggingUtil loggingUtil;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private HttpServletRequest request;
    
    @GetMapping
    public String viewProfile(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser user = hospuserRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Add password expiry calculation here in controller
            if (user.getLastPasswordChange() != null) {
                long daysSinceChange = java.time.temporal.ChronoUnit.DAYS.between(
                    user.getLastPasswordChange().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate(),
                    java.time.LocalDate.now()
                );
                model.addAttribute("daysSincePasswordChange", daysSinceChange);
                model.addAttribute("daysUntilExpiry", 90 - daysSinceChange);
                model.addAttribute("passwordExpiringSoon", daysSinceChange > 80);
            }
            
            model.addAttribute("user", user);
            return "profile/view";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading profile. Please try again.");
            return "redirect:/";
        }
    }
    
    @GetMapping("/edit")
    public String editProfile(Authentication authentication, Model model) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser user = hospuserRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            model.addAttribute("user", user);
            return "profile/edit";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading profile: " + e.getMessage());
            return "redirect:/profile";
        }
    }
    
    @PostMapping("/edit")
    public String updateProfile(Authentication authentication,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfBirth,
                               RedirectAttributes redirectAttributes) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser user = hospuserRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate input
            if (validationUtil.containsSQLInjection(firstName) || validationUtil.containsXSS(firstName) ||
                validationUtil.containsSQLInjection(lastName) || validationUtil.containsXSS(lastName)) {
                redirectAttributes.addFlashAttribute("error", "Invalid input detected");
                return "redirect:/profile/edit";
            }
            
            user.setFirstName(validationUtil.sanitizeInput(firstName));
            user.setLastName(validationUtil.sanitizeInput(lastName));
            user.setDateOfBirth(dateOfBirth);
            
            hospuserRepository.save(user);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
            return "redirect:/profile";
        } catch (Exception e) {
            e.printStackTrace(); // For debugging
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/profile/edit";
        }
    }
    
    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "profile/change-password";
    }
    
    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                @RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser user = hospuserRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/profile/change-password";
            }
            
            // Check if trying to reuse current password
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your new password must be different from your current password.");
                return "redirect:/profile/change-password";
            }
            
            // Check if passwords match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match");
                return "redirect:/profile/change-password";
            }
            
            // Validate new password
            PasswordValidator.ValidationResult validation = passwordValidator.validate(newPassword);
            if (validation.hasErrors()) {
                redirectAttributes.addFlashAttribute("error", String.join(", ", validation.getErrors()));
                return "redirect:/profile/change-password";
            }
            
            // Check password history (last 5 passwords)
            if (passwordValidator.isPasswordInHistory(newPassword, user.getPasswordHistory())) {
                redirectAttributes.addFlashAttribute("error", 
                    "This password has been used recently. You cannot reuse any of your last 5 passwords. Please choose a different password.");
                return "redirect:/profile/change-password";
            }
            
            // Update password and history
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHistory(passwordValidator.updatePasswordHistory(hashedPassword, user.getPasswordHistory()));
            user.setPassword(hashedPassword);
            user.setLastPasswordChange(new Date());
            user.setForcePasswordChange(false);
            
            hospuserRepository.save(user);
            
            // Log the password change
            loggingUtil.logSecurityEvent("PASSWORD_CHANGED", user.getUsername(), true);
            auditLogService.logPasswordChange(user.getUsername(), true);
            
            // Clear the security context to force re-login
            SecurityContextHolder.clearContext();
            
            // Set success message for login page
            redirectAttributes.addFlashAttribute("MSG_SUCCESS", 
                "Password changed successfully! Please login with your new password.");
            
            return "redirect:/auth/login";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "An error occurred while changing your password. Please try again.");
            return "redirect:/profile/change-password";
        }
    }
    
    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
