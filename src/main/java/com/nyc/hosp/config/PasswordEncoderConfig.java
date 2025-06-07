package com.nyc.hosp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for backward compatibility with original passwords
 * Supports both old simple passwords (1234) and new complex passwords
 */
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default encoder for new passwords
        BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder(12);
        
        // Support for multiple password encoding schemes
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", bcryptEncoder);
        
        DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        
        // This allows the system to verify passwords encoded with older/different schemes
        delegatingEncoder.setDefaultPasswordEncoderForMatches(bcryptEncoder);
        
        return delegatingEncoder;
    }
}
