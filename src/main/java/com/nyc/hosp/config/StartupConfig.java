package com.nyc.hosp.config;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Startup configuration to check and encourage password updates
 */
@Configuration
public class StartupConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupConfig.class);
    
    @Bean
    CommandLineRunner init(HospuserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            logger.info("=== Hospital Management System Started ===");
            logger.info("Original users can login with password: 1234");
            logger.info("New secure password policy requires: Password123! format");
            logger.info("=========================================");
            
            // Check if we need to create any missing data
            long userCount = userRepository.count();
            if (userCount == 0) {
                logger.warn("No users found in database. Please run the import.sql script.");
            } else {
                logger.info("Found {} users in the database", userCount);
            }
        };
    }
}
