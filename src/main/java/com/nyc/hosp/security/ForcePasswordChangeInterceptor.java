package com.nyc.hosp.security;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ForcePasswordChangeInterceptor implements HandlerInterceptor {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Skip check for these paths
        if (requestURI.startsWith("/auth/") || 
            requestURI.startsWith("/api/") ||
            requestURI.startsWith("/css/") ||
            requestURI.startsWith("/js/") ||
            requestURI.startsWith("/images/") ||
            requestURI.equals("/force-password-change") ||
            requestURI.equals("/password-change-success") ||
            requestURI.equals("/error")) {
            return true;
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            Hospuser user = hospuserRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null && user.isForcePasswordChange()) {
                response.sendRedirect("/force-password-change");
                return false;
            }
        }
        
        return true;
    }
}
