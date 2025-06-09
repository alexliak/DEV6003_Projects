package com.nyc.hosp.util;

import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migrates existing plain text diagnoses to encrypted format
 * This should run once on application startup
 */
@Component
public class EncryptionMigration implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionMigration.class);
    
    @Autowired
    private PatientvisitRepository repository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting encryption migration...");
        
        // Find all visits with plain text diagnosis
        List<Patientvisit> visitsToEncrypt = repository.findAll().stream()
            .filter(v -> v.getDiagnosis() != null && v.getEncryptedDiagnosis() == null)
            .toList();
        
        if (visitsToEncrypt.isEmpty()) {
            logger.info("No plain text diagnoses found. Migration not needed.");
            return;
        }
        
        logger.info("Found {} visits with plain text diagnosis. Encrypting...", visitsToEncrypt.size());
        
        int encrypted = 0;
        for (Patientvisit visit : visitsToEncrypt) {
            try {
                String plainDiagnosis = visit.getDiagnosis();
                String encryptedDiagnosis = encryptionService.encrypt(plainDiagnosis);
                
                visit.setEncryptedDiagnosis(encryptedDiagnosis);
                visit.setDiagnosis(null); // Remove plain text
                
                repository.save(visit);
                encrypted++;
                logger.debug("Encrypted diagnosis for visit ID: {}", visit.getVisitid());
            } catch (Exception e) {
                logger.error("Failed to encrypt visit ID: {}", visit.getVisitid(), e);
            }
        }
        
        logger.info("Encryption migration completed. Encrypted {} out of {} visits.", 
                    encrypted, visitsToEncrypt.size());
    }
}
