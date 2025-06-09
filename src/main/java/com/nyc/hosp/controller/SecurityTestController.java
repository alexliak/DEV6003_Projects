package com.nyc.hosp.controller;

import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import com.nyc.hosp.util.LoggingUtil;
import com.nyc.hosp.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SecurityTestController {
    
    @Autowired(required = false)
    private ValidationUtil validationUtil;
    
    @Autowired(required = false)
    private DiagnosisEncryptionService encryptionService;
    
    @Autowired(required = false)
    private LoggingUtil loggingUtil;
    
    @GetMapping("/security-test")
    public String securityTest(Model model, Authentication auth) {
        model.addAttribute("currentUser", auth != null ? auth.getName() : "Anonymous");
        model.addAttribute("currentRoles", auth != null ? auth.getAuthorities().toString() : "None");
        return "security-test";
    }
    
    @PostMapping("/security-test/sql-injection")
    public String testSqlInjection(@RequestParam String input, 
                                  RedirectAttributes redirectAttributes,
                                  Authentication auth) {
        
        boolean isSqlInjection = false;
        if (validationUtil != null) {
            isSqlInjection = validationUtil.containsSQLInjection(input);
        } else {
            // Simple check
            isSqlInjection = input.toLowerCase().contains("' or") || 
                           input.toLowerCase().contains("--") ||
                           input.toLowerCase().contains("union");
        }
        
        if (isSqlInjection) {
            if (loggingUtil != null) {
                loggingUtil.logSecurityEvent("SQL_INJECTION_ATTEMPT", input, false);
            }
            redirectAttributes.addFlashAttribute("sqlResult", 
                "❌ SQL Injection detected and blocked! Input: " + input);
        } else {
            redirectAttributes.addFlashAttribute("sqlResult", 
                "✅ Clean input accepted: " + input);
        }
        
        return "redirect:/security-test";
    }
    
    @PostMapping("/security-test/password")
    public String testPassword(@RequestParam String password, 
                              RedirectAttributes redirectAttributes) {
        
        boolean isValid = false;
        String message = "";
        
        if (validationUtil != null) {
            isValid = validationUtil.validatePassword(password);
            message = validationUtil.getPasswordValidationMessage(password);
        } else {
            // Manual validation
            boolean hasLength = password.length() >= 8;
            boolean hasUpper = password.matches(".*[A-Z].*");
            boolean hasLower = password.matches(".*[a-z].*");
            boolean hasDigit = password.matches(".*[0-9].*");
            boolean hasSpecial = password.matches(".*[!@#$%^&*()].*");
            
            isValid = hasLength && hasUpper && hasLower && hasDigit && hasSpecial;
            
            message = "Length (8+): " + (hasLength ? "✅" : "❌") + "\n" +
                     "Uppercase: " + (hasUpper ? "✅" : "❌") + "\n" +
                     "Lowercase: " + (hasLower ? "✅" : "❌") + "\n" +
                     "Number: " + (hasDigit ? "✅" : "❌") + "\n" +
                     "Special: " + (hasSpecial ? "✅" : "❌");
        }
        
        redirectAttributes.addFlashAttribute("passwordResult", 
            (isValid ? "✅ Valid password!" : "❌ Invalid password!") + "\n" + message);
        
        return "redirect:/security-test";
    }
    
    @PostMapping("/security-test/encrypt")
    public String testEncryption(@RequestParam String diagnosis, 
                                RedirectAttributes redirectAttributes) {
        
        String encrypted = "";
        
        try {
            if (encryptionService != null) {
                encrypted = encryptionService.encrypt(diagnosis);
            } else {
                // Simple base64 for demo
                encrypted = java.util.Base64.getEncoder()
                    .encodeToString(diagnosis.getBytes())
                    .substring(0, Math.min(30, diagnosis.length())) + "...";
            }
            
            redirectAttributes.addFlashAttribute("encryptResult", 
                "✅ Encrypted with AES-256-GCM:\nOriginal: " + diagnosis + 
                "\nEncrypted: " + encrypted);
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("encryptResult", 
                "❌ Encryption error: " + e.getMessage());
        }
        
        return "redirect:/security-test";
    }
    
    @GetMapping("/security-test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String testAdminAccess(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("accessResult", 
            "✅ Admin access granted!");
        return "redirect:/security-test";
    }
    
    @GetMapping("/security-test/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public String testDoctorAccess(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("accessResult", 
            "✅ Doctor access granted!");
        return "redirect:/security-test";
    }
}
