package com.nyc.hosp.config;

import com.nyc.hosp.security.ForcePasswordChangeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private ForcePasswordChangeInterceptor forcePasswordChangeInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forcePasswordChangeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/api/**", "/css/**", "/js/**", 
                                   "/images/**", "/force-password-change", "/password-change-success", "/error");
    }
}
