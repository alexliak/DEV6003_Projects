package com.nyc.hosp.auth;

import com.nyc.hosp.auth.dto.JwtResponse;
import com.nyc.hosp.auth.dto.LoginRequest;
import com.nyc.hosp.auth.dto.PasswordChangeRequest;
import com.nyc.hosp.audit.AuditLogService;
import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.security.JwtTokenProvider;
import com.nyc.hosp.validation.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Value("${security.password.max-attempts:3}")
    private int maxLoginAttempts;
    
    @Value("${security.lockout-duration:900000}")
    private long lockoutDuration;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, 
                                            HttpServletRequest request) {
        String username = loginRequest.getUsernameOrEmail();
        logger.info("Login attempt for user: {}", username);
        
        // Check if account is locked
        Hospuser user = hospuserRepository.findByUsernameOrEmail(username, username).orElse(null);
        if (user == null) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
        
        logger.info("User found: {}, password in DB: {}", user.getUsername(), user.getPassword());
        logger.info("Roles: {}", user.getRoles().size());
        
        if (user.isAccountLocked()) {
            auditLogService.logAuthenticationEvent(username, "LOGIN_ATTEMPT_LOCKED", false, request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account is locked due to too many failed login attempts"));
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            // Reset failed login attempts on successful login
            if (user != null && user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                hospuserRepository.save(user);
            }
            
            auditLogService.logAuthenticationEvent(username, "LOGIN_SUCCESS", true, request.getRemoteAddr());
            
            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    roles
            ));
            
        } catch (BadCredentialsException e) {
            // Handle failed login attempt
            if (user != null) {
                int failedAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(failedAttempts);
                
                if (failedAttempts >= maxLoginAttempts) {
                    user.setAccountLocked(true);
                    auditLogService.logAuthenticationEvent(username, "ACCOUNT_LOCKED", false, request.getRemoteAddr());
                }
                
                hospuserRepository.save(user);
            }
            
            auditLogService.logAuthenticationEvent(username, "LOGIN_FAILED", false, request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        Hospuser user = hospuserRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Current password is incorrect"));
        }
        
        // Validate new password
        var validationResult = passwordValidator.validate(request.getNewPassword());
        if (!validationResult.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Password does not meet requirements", 
                               "errors", validationResult.getErrors()));
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(new Date());
        hospuserRepository.save(user);
        
        auditLogService.logAuthenticationEvent(user.getUsername(), "PASSWORD_CHANGED", true, "");
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication != null) {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            auditLogService.logAuthenticationEvent(userPrincipal.getUsername(), "LOGOUT", true, "");
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
