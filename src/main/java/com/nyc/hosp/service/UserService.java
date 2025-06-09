package com.nyc.hosp.service;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.validation.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private PasswordValidator passwordValidator;
    
    @Value("${security.max-login-attempts:3}")
    private int maxFailedAttempts;
    
    @Value("${security.lockout-duration:900000}")
    private long lockoutDuration;
    
    public boolean isAccountLocked(String usernameOrEmail) {
        Hospuser user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElse(null);
        
        if (user == null) {
            return false;
        }
        
        if (user.isAccountLocked()) {
            // Check if lockout period has expired
            if (user.getLockTime() != null) {
                long lockTimeInMillis = user.getLockTime().getTime();
                long currentTimeInMillis = System.currentTimeMillis();
                
                if (lockTimeInMillis + lockoutDuration < currentTimeInMillis) {
                    // Unlock the account
                    user.setAccountLocked(false);
                    user.setLockTime(null);
                    user.setFailedLoginAttempts(0);
                    userRepository.save(user);
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }
    
    public void incrementFailedAttempts(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .ifPresent(user -> {
                int newFailAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(newFailAttempts);
                
                if (newFailAttempts >= maxFailedAttempts) {
                    user.setAccountLocked(true);
                    user.setLockTime(new Date());
                    logger.warn("Account locked for user: {} after {} failed attempts", 
                        user.getUsername(), newFailAttempts);
                }
                
                userRepository.save(user);
            });
    }
    
    public void resetFailedAttempts(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .ifPresent(user -> {
                user.setFailedLoginAttempts(0);
                user.setAccountLocked(false);
                user.setLockTime(null);
                userRepository.save(user);
            });
    }
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        return userRepository.findByUsername(username)
            .map(user -> {
                // Verify old password
                if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                    logger.warn("Password change failed for user {}: incorrect old password", username);
                    return false;
                }
                
                // Check if trying to reuse current password
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                    logger.warn("Password change failed for user {}: trying to reuse current password", username);
                    return false;
                }
                
                // Check password history (last 5 passwords)
                if (passwordValidator.isPasswordInHistory(newPassword, user.getPasswordHistory())) {
                    logger.warn("Password change failed for user {}: password found in history", username);
                    return false;
                }
                
                // Update password and history
                String hashedPassword = passwordEncoder.encode(newPassword);
                user.setPasswordHistory(passwordValidator.updatePasswordHistory(hashedPassword, user.getPasswordHistory()));
                user.setPassword(hashedPassword);
                user.setLastPasswordChange(new Date());
                user.setForcePasswordChange(false);
                userRepository.save(user);
                
                logger.info("Password changed successfully for user: {}", username);
                return true;
            })
            .orElse(false);
    }
    
    public boolean isPasswordExpired(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                if (user.getLastPasswordChange() == null) {
                    return true; // No password change date, consider expired
                }
                
                long daysSinceChange = (System.currentTimeMillis() - user.getLastPasswordChange().getTime()) 
                    / (1000 * 60 * 60 * 24);
                
                return daysSinceChange > 90; // 90 days policy
            })
            .orElse(false);
    }
}
