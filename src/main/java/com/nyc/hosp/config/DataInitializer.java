package com.nyc.hosp.config;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private PatientvisitRepository patientvisitRepository;
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Autowired
    private DiagnosisEncryptionService encryptionService;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Check if we have any visits
        long visitCount = patientvisitRepository.count();
        logger.info("Current patient visits in database: {}", visitCount);
        
        if (visitCount == 0) {
            logger.info("No patient visits found. Creating sample data...");
            
            // Find patients and doctors
            List<Hospuser> patients = hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_PATIENT);
            List<Hospuser> doctors = hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_DOCTOR);
            
            logger.info("Found {} patients and {} doctors", patients.size(), doctors.size());
            
            if (!patients.isEmpty() && !doctors.isEmpty()) {
                // Create sample visits
                createSampleVisit(patients.get(0), doctors.get(0), 
                    "Patient complains of headache and mild fever. Prescribed paracetamol.");
                
                createSampleVisit(patients.get(0), doctors.get(0), 
                    "Follow-up visit. Fever has subsided. Patient recovering well.");
                
                if (doctors.size() > 1) {
                    createSampleVisit(patients.get(0), doctors.get(1), 
                        "Annual check-up. All vitals normal. Advised to continue healthy lifestyle.");
                }
                
                logger.info("Sample patient visits created successfully! Total visits now: {}", 
                    patientvisitRepository.count());
            } else {
                logger.warn("Not enough patients or doctors to create sample visits");
                logger.warn("Patients: {}, Doctors: {}", patients.size(), doctors.size());
            }
        } else {
            logger.info("Patient visits already exist: {}", visitCount);
        }
    }
    
    private void createSampleVisit(Hospuser patient, Hospuser doctor, String diagnosis) {
        try {
            Patientvisit visit = new Patientvisit();
            visit.setPatient(patient);
            visit.setDoctor(doctor);
            visit.setVisitdate(LocalDate.now().minusDays((long)(Math.random() * 30)));
            visit.setDiagnosis(diagnosis);
            
            patientvisitRepository.save(visit);
            logger.info("Created visit for patient {} with doctor {}", 
                patient.getUsername(), doctor.getUsername());
        } catch (Exception e) {
            logger.error("Failed to create sample visit", e);
        }
    }
}
