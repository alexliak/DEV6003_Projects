package com.nyc.hosp.security;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationProvider.class);
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usernameOrEmail = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        logger.debug("Attempting authentication for user: {}", usernameOrEmail);
        
        // Check if account is locked FIRST
        if (userService.isAccountLocked(usernameOrEmail)) {
            logger.warn("Locked account login attempt for user: {}", usernameOrEmail);
            throw new LockedException("Account is locked due to multiple failed login attempts. Please contact administrator or wait 15 minutes.");
        }
        
        // Find user
        Hospuser user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElse(null);
        
        // Check if user exists
        if (user == null) {
            logger.debug("User not found: {}", usernameOrEmail);
            // Still increment failed attempts for security
            userService.incrementFailedAttempts(usernameOrEmail);
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.debug("Invalid password for user: {}", usernameOrEmail);
            // Increment failed attempts
            userService.incrementFailedAttempts(usernameOrEmail);
            
            // Check if account is now locked after this attempt
            if (userService.isAccountLocked(usernameOrEmail)) {
                throw new LockedException("Account has been locked due to 3 failed login attempts. Please wait 15 minutes.");
            }
            
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Reset failed attempts only if authentication is successful
        userService.resetFailedAttempts(usernameOrEmail);
        
        // Load user details and create authentication token
        CustomUserPrincipal userDetails = (CustomUserPrincipal) userDetailsService.loadUserByUsername(usernameOrEmail);
        
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
