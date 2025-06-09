package com.nyc.hosp.service;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.service.EncryptionService;
import com.nyc.hosp.model.PatientvisitDTO;
import com.nyc.hosp.model.PatientvisitDisplayDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.util.NotFoundException;
import com.nyc.hosp.validation.InputSanitizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class PatientvisitService {

    private static final Logger logger = LoggerFactory.getLogger(PatientvisitService.class);
    
    private final PatientvisitRepository patientvisitRepository;
    private final HospuserRepository hospuserRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private InputSanitizer inputSanitizer;

    public PatientvisitService(final PatientvisitRepository patientvisitRepository,
            final HospuserRepository hospuserRepository) {
        this.patientvisitRepository = patientvisitRepository;
        this.hospuserRepository = hospuserRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientvisitDTO> findAll() {
        final List<Patientvisit> patientvisits = patientvisitRepository.findAllWithPatientAndDoctor();
        return patientvisits.stream()
                .map(patientvisit -> mapToDTO(patientvisit, new PatientvisitDTO()))
                .toList();
    }
    
    public List<PatientvisitDisplayDTO> findAllForDisplay() {
        final List<Patientvisit> patientvisits = patientvisitRepository.findAll(Sort.by("visitid").descending());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;
        
        return patientvisits.stream()
                .map(patientvisit -> {
                    PatientvisitDisplayDTO dto = new PatientvisitDisplayDTO();
                    mapToDTO(patientvisit, dto);
                    
                    // Set patient and doctor names
                    if (patientvisit.getPatient() != null) {
                        dto.setPatientName(patientvisit.getPatient().getUsername() + 
                            " (" + patientvisit.getPatient().getEmail() + ")");
                    }
                    if (patientvisit.getDoctor() != null) {
                        dto.setDoctorName("Dr. " + patientvisit.getDoctor().getUsername());
                    }
                    
                    // Check if current user can edit (only the doctor who created it)
                    if (patientvisit.getDoctor() != null && currentUsername != null) {
                        dto.setCanEdit(patientvisit.getDoctor().getUsername().equals(currentUsername) ||
                                      auth.getAuthorities().stream()
                                          .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
                    }
                    
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientvisitDTO get(final Integer visitid) {
        return patientvisitRepository.findByIdWithPatientAndDoctor(visitid)
                .map(patientvisit -> mapToDTO(patientvisit, new PatientvisitDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Integer create(final PatientvisitDTO patientvisitDTO) {
        // VALIDATE INPUT BEFORE SAVING
        if (patientvisitDTO.getDiagnosis() != null && !inputSanitizer.isValid(patientvisitDTO.getDiagnosis())) {
            logger.error("Malicious input detected in diagnosis: {}", patientvisitDTO.getDiagnosis());
            throw new IllegalArgumentException("Invalid input detected. This incident has been logged.");
        }
        
        // Sanitize the diagnosis
        if (patientvisitDTO.getDiagnosis() != null) {
            patientvisitDTO.setDiagnosis(inputSanitizer.sanitize(patientvisitDTO.getDiagnosis()));
        }
        
        final Patientvisit patientvisit = new Patientvisit();
        mapToEntity(patientvisitDTO, patientvisit);
        
        logger.info("Creating visit with encrypted diagnosis");
        
        return patientvisitRepository.save(patientvisit).getVisitid();
    }

    public void update(final Integer visitid, final PatientvisitDTO patientvisitDTO) {
        // VALIDATE INPUT BEFORE SAVING
        if (patientvisitDTO.getDiagnosis() != null && !inputSanitizer.isValid(patientvisitDTO.getDiagnosis())) {
            logger.error("Malicious input detected in diagnosis during update: {}", patientvisitDTO.getDiagnosis());
            throw new IllegalArgumentException("Invalid input detected. This incident has been logged.");
        }
        
        // Sanitize the diagnosis
        if (patientvisitDTO.getDiagnosis() != null) {
            patientvisitDTO.setDiagnosis(inputSanitizer.sanitize(patientvisitDTO.getDiagnosis()));
        }
        
        final Patientvisit patientvisit = patientvisitRepository.findById(visitid)
                .orElseThrow(NotFoundException::new);
        mapToEntity(patientvisitDTO, patientvisit);
        
        patientvisitRepository.save(patientvisit);
    }

    public void delete(final Integer visitid) {
        patientvisitRepository.deleteById(visitid);
    }

    private PatientvisitDTO mapToDTO(final Patientvisit patientvisit,
            final PatientvisitDTO patientvisitDTO) {
        patientvisitDTO.setVisitid(patientvisit.getVisitid());
        patientvisitDTO.setVisitdate(patientvisit.getVisitdate());
        
        // Handle diagnosis decryption
        if (patientvisit.getEncryptedDiagnosis() != null && !patientvisit.getEncryptedDiagnosis().isEmpty()) {
            try {
                String decrypted = encryptionService.decrypt(patientvisit.getEncryptedDiagnosis());
                patientvisitDTO.setDiagnosis(decrypted);
            } catch (Exception e) {
                logger.error("Failed to decrypt diagnosis for visit {}", patientvisit.getVisitid(), e);
                patientvisitDTO.setDiagnosis("[Decryption Failed]");
            }
        } else if (patientvisit.getDiagnosis() != null) {
            // Legacy plain text (should not exist in production)
            patientvisitDTO.setDiagnosis(patientvisit.getDiagnosis());
        } else {
            patientvisitDTO.setDiagnosis("");
        }
        
        patientvisitDTO.setPatient(patientvisit.getPatient() == null ? null : patientvisit.getPatient().getId().intValue());
        patientvisitDTO.setDoctor(patientvisit.getDoctor() == null ? null : patientvisit.getDoctor().getId().intValue());
        
        // Set names for display
        if (patientvisit.getPatient() != null) {
            Hospuser patient = patientvisit.getPatient();
            patientvisitDTO.setPatientName(patient.getFirstName() + " " + patient.getLastName() + " (" + patient.getUsername() + ")");
            if (patient.getDateOfBirth() != null) {
                int age = java.time.Period.between(
                    new java.sql.Date(patient.getDateOfBirth().getTime()).toLocalDate(), 
                    java.time.LocalDate.now()
                ).getYears();
                patientvisitDTO.setPatientAge(age);
            }
        }
        
        if (patientvisit.getDoctor() != null) {
            Hospuser doctor = patientvisit.getDoctor();
            patientvisitDTO.setDoctorName("Dr. " + doctor.getFirstName() + " " + doctor.getLastName());
            patientvisitDTO.setDoctorUsername(doctor.getUsername());
        }
        
        // Format visit date
        if (patientvisit.getVisitdate() != null) {
            patientvisitDTO.setFormattedVisitDate(
                patientvisit.getVisitdate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        }
        
        return patientvisitDTO;
    }

    private Patientvisit mapToEntity(final PatientvisitDTO patientvisitDTO,
            final Patientvisit patientvisit) {
        patientvisit.setVisitdate(patientvisitDTO.getVisitdate());
        
        // ALWAYS encrypt diagnosis before saving
        if (patientvisitDTO.getDiagnosis() != null && !patientvisitDTO.getDiagnosis().trim().isEmpty()) {
            try {
                String encrypted = encryptionService.encrypt(patientvisitDTO.getDiagnosis());
                patientvisit.setEncryptedDiagnosis(encrypted);
                patientvisit.setDiagnosis(null); // NEVER store plain text
                logger.info("Successfully encrypted diagnosis for visit");
            } catch (Exception e) {
                logger.error("Failed to encrypt diagnosis", e);
                throw new RuntimeException("Encryption is mandatory for medical data");
            }
        } else {
            patientvisit.setEncryptedDiagnosis(null);
            patientvisit.setDiagnosis(null);
        }
        
        final Hospuser patient = patientvisitDTO.getPatient() == null ? null : hospuserRepository.findById(patientvisitDTO.getPatient().longValue())
                .orElseThrow(() -> new NotFoundException("patient not found"));
        patientvisit.setPatient(patient);
        final Hospuser doctor = patientvisitDTO.getDoctor() == null ? null : hospuserRepository.findById(patientvisitDTO.getDoctor().longValue())
                .orElseThrow(() -> new NotFoundException("doctor not found"));
        patientvisit.setDoctor(doctor);
        return patientvisit;
    }

}
