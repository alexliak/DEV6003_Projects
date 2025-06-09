package com.nyc.hosp.controller;

import com.nyc.hosp.dto.JwtResponse;
import com.nyc.hosp.dto.LoginRequest;
import com.nyc.hosp.dto.PasswordChangeRequest;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.security.JwtTokenProvider;
import com.nyc.hosp.service.UserService;
import com.nyc.hosp.validation.PasswordValidator;
import com.nyc.hosp.security.SecurityEventLogger;
import com.nyc.hosp.repos.HospuserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
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
    private UserService userService;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Autowired
    private SecurityEventLogger securityEventLogger;
    
    @Autowired
    private HospuserRepository userRepository;
    
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Check if account is locked
            if (userService.isAccountLocked(loginRequest.getUsernameOrEmail())) {
                securityEventLogger.logEvent("AUTHENTICATION", loginRequest.getUsernameOrEmail(), 
                    "LOGIN_ATTEMPT", "FAILED", "Account is locked");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Account locked due to multiple failed login attempts"));
            }
            
            // For API calls, expect plain password
            // For web form submissions with hashed password, handle in WebAuthController
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Reset failed attempts on successful login
            userService.resetFailedAttempts(loginRequest.getUsernameOrEmail());
            
            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);
            
            // Get user details
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // Check if user needs to change password
            var user = userRepository.findByUsername(userPrincipal.getUsername()).orElse(null);
            if (user != null && user.isForcePasswordChange()) {
                logger.warn("User {} needs to change password", userPrincipal.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "message", "Password change required",
                        "requiresPasswordChange", true,
                        "redirectUrl", "/force-password-change"
                    ));
            }
            
            // Update last login time
            if (user != null) {
                user.setLastlogondatetime(OffsetDateTime.now());
                userRepository.save(user);
            }
            
            List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            
            logger.info("User {} logged in successfully", userPrincipal.getUsername());
            securityEventLogger.logEvent("AUTHENTICATION", userPrincipal.getUsername(), 
                "LOGIN", "SUCCESS", "Roles: " + roles);
            
            return ResponseEntity.ok(new JwtResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                roles
            ));
            
        } catch (BadCredentialsException e) {
            // Increment failed attempts
            userService.incrementFailedAttempts(loginRequest.getUsernameOrEmail());
            
            logger.warn("Failed login attempt for user: {}", loginRequest.getUsernameOrEmail());
            securityEventLogger.logEvent("AUTHENTICATION", loginRequest.getUsernameOrEmail(), 
                "LOGIN_ATTEMPT", "FAILED", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
        } catch (Exception e) {
            logger.error("Error during authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Authentication error"));
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          Authentication authentication) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // Validate new password complexity
            var validationResult = passwordValidator.validate(request.getNewPassword());
            if (validationResult.hasErrors()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Password does not meet complexity requirements",
                        "errors", validationResult.getErrors()
                    ));
            }
            
            // Change password
            boolean success = userService.changePassword(
                principal.getUsername(), 
                request.getOldPassword(), 
                request.getNewPassword()
            );
            
            if (success) {
                logger.info("Password changed successfully for user: {}", principal.getUsername());
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid old password"));
            }
            
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error changing password"));
        }
    }
}
