package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.validation.PasswordValidator;
import com.nyc.hosp.util.LoggingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.Date;

@Controller
public class ForcePasswordChangeController {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private LoggingUtil loggingUtil;
    
    @GetMapping("/force-password-change")
    public String showForcePasswordChange(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "auth/force-password-change";
    }
    
    @PostMapping("/force-password-change")
    public String handleForcePasswordChange(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestParam String newPassword,
                                          @RequestParam String confirmPassword,
                                          RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/force-password-change";
            }
            
            PasswordValidator.ValidationResult validationResult = passwordValidator.validate(newPassword);
            if (!validationResult.isValid()) {
                redirectAttributes.addFlashAttribute("error", String.join("<br>", validationResult.getErrors()));
                return "redirect:/force-password-change";
            }
            
            Hospuser user = hospuserRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if trying to reuse current password
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", 
                    "Your new password must be different from your current password.");
                return "redirect:/force-password-change";
            }
            
            // Check password history (last 5 passwords)
            if (passwordValidator.isPasswordInHistory(newPassword, user.getPasswordHistory())) {
                redirectAttributes.addFlashAttribute("error", 
                    "This password has been used recently. You cannot reuse any of your last 5 passwords. Please choose a different password.");
                return "redirect:/force-password-change";
            }
            
            // Update password and history
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPasswordHistory(passwordValidator.updatePasswordHistory(hashedPassword, user.getPasswordHistory()));
            user.setPassword(hashedPassword);
            user.setLastPasswordChange(new Date());
            user.setForcePasswordChange(false);
            // Update last login time now that password is changed
            user.setLastlogondatetime(OffsetDateTime.now());
            hospuserRepository.save(user);
            
            loggingUtil.logSecurityEvent("FORCED_PASSWORD_CHANGED", userDetails.getUsername(), true);
            
            // Redirect to success page
            return "redirect:/password-change-success";
        } catch (Exception e) {
            loggingUtil.logSecurityEvent("FORCED_PASSWORD_CHANGE_FAILED", userDetails.getUsername(), false);
            redirectAttributes.addFlashAttribute("error", "Failed to change password");
            return "redirect:/force-password-change";
        }
    }
}
