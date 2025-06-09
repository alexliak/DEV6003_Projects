package com.nyc.hosp.security;

import com.nyc.hosp.service.UserService;
import com.nyc.hosp.util.LoggingUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LoggingUtil loggingUtil;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
        
        String username = request.getParameter("usernameOrEmail");
        
        logger.debug("Authentication failed for user: {}", username);
        
        if (username != null) {
            // Log the failed attempt
            loggingUtil.logSecurityEvent("LOGIN_FAILED", username, false);
        }
        
        // Check if it's a locked account exception
        if (exception instanceof LockedException) {
            // Redirect to account locked page
            getRedirectStrategy().sendRedirect(request, response, "/auth/account-locked");
            return;
        }
        
        // For other failures, redirect to login with error
        super.setDefaultFailureUrl("/auth/login?error=true");
        super.onAuthenticationFailure(request, response, exception);
    }
}
