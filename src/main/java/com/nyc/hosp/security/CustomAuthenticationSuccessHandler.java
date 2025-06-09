package com.nyc.hosp.security;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        // Get the user
        String username = authentication.getName();
        Hospuser user = userRepository.findByUsername(username).orElse(null);
        
        if (user != null) {
            // Check if user needs to change password
            if (user.isForcePasswordChange()) {
                logger.info("User {} requires password change", username);
                // Don't update login time yet
                response.sendRedirect("/force-password-change");
                return;
            }
            
            // Update last login time only if not forced to change password
            user.setLastlogondatetime(OffsetDateTime.now());
            userRepository.save(user);
            logger.info("Updated last login time for user: {}", username);
        }
        
        // Set default target URL
        setDefaultTargetUrl("/");
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
