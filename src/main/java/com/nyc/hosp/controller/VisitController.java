package com.nyc.hosp.controller;

import com.nyc.hosp.audit.AuditLogService;
import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.dto.VisitDTO;
import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.security.SecurityEventLogger;
import com.nyc.hosp.validation.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitController.class);
    
    @Autowired
    private PatientvisitRepository visitRepository;
    
    @Autowired
    private HospuserRepository userRepository;
    
    @Autowired
    private DiagnosisEncryptionService encryptionService;
    
    @Autowired
    private InputSanitizer inputSanitizer;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private SecurityEventLogger securityEventLogger;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> createVisit(@Valid @RequestBody VisitDTO visitDTO, 
                                       Authentication authentication) {
        try {
            // Validate and sanitize input
            if (!inputSanitizer.isValid(visitDTO.getDiagnosis())) {
                logger.warn("Malicious input detected from user: {}", authentication.getName());
                auditLogService.logSecurityViolation(authentication.getName(), 
                    "MALICIOUS_INPUT", "Attempted SQL injection or XSS in diagnosis", 
                    request.getRemoteAddr());
                securityEventLogger.logEvent("SECURITY_VIOLATION", authentication.getName(),
                    "MALICIOUS_INPUT_ATTEMPT", "BLOCKED", "SQL injection or XSS detected in diagnosis field");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid input detected"));
            }
            
            String sanitizedDiagnosis = inputSanitizer.sanitize(visitDTO.getDiagnosis());
            
            // Get doctor user
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser doctor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
            
            // Get patient
            Hospuser patient = userRepository.findById(visitDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
            
            // Create visit
            Patientvisit visit = new Patientvisit();
            visit.setVisitdate(LocalDate.now());
            visit.setPatient(patient);
            visit.setDoctor(doctor);
            
            // Encrypt diagnosis
            String encryptedDiagnosis = encryptionService.encrypt(sanitizedDiagnosis);
            visit.setEncryptedDiagnosis(encryptedDiagnosis);
            
            visitRepository.save(visit);
            
            // Audit log
            auditLogService.logDataAccess(authentication.getName(), "Visit", 
                visit.getVisitid().longValue(), "CREATE", true);
            logger.info("Visit created by doctor: {} for patient: {}", doctor.getUsername(), patient.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", visit.getVisitid());
            response.put("message", "Visit created successfully");
            response.put("visitDate", visit.getVisitdate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error creating visit"));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> updateVisit(@PathVariable Integer id,
                                       @Valid @RequestBody VisitDTO visitDTO,
                                       Authentication authentication) {
        try {
            Patientvisit visit = visitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visit not found"));
            
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            
            // Check if doctor can edit this visit (only their own)
            if (!principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                if (!visit.getDoctor().getId().equals(principal.getId())) {
                    auditLogService.logSecurityViolation(authentication.getName(), 
                        "UNAUTHORIZED_ACCESS", "Attempted to update another doctor's visit", 
                        request.getRemoteAddr());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only edit your own visits"));
                }
            }
            
            // Validate and sanitize input
            if (!inputSanitizer.isValid(visitDTO.getDiagnosis())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid input detected"));
            }
            
            String sanitizedDiagnosis = inputSanitizer.sanitize(visitDTO.getDiagnosis());
            
            // Update encrypted diagnosis
            String encryptedDiagnosis = encryptionService.encrypt(sanitizedDiagnosis);
            visit.setEncryptedDiagnosis(encryptedDiagnosis);
            
            visitRepository.save(visit);
            
            // Audit log
            auditLogService.logDataAccess(authentication.getName(), "Visit", 
                visit.getVisitid().longValue(), "UPDATE", true);
            
            return ResponseEntity.ok(Map.of("message", "Visit updated successfully"));
            
        } catch (Exception e) {
            logger.error("Error updating visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error updating visit"));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getAllVisits(Authentication authentication) {
        try {
            List<Patientvisit> visits = visitRepository.findAll();
            
            // Decrypt diagnoses for authorized users
            List<Map<String, Object>> visitList = visits.stream().map(visit -> {
                Map<String, Object> visitMap = new HashMap<>();
                visitMap.put("id", visit.getVisitid());
                visitMap.put("visitDate", visit.getVisitdate());
                visitMap.put("patientName", visit.getPatient().getUsername());
                visitMap.put("doctorName", visit.getDoctor().getUsername());
                
                try {
                    if (visit.getEncryptedDiagnosis() != null) {
                        String decryptedDiagnosis = encryptionService.decrypt(visit.getEncryptedDiagnosis());
                        visitMap.put("diagnosis", decryptedDiagnosis);
                    } else {
                        visitMap.put("diagnosis", "No diagnosis recorded");
                    }
                } catch (Exception e) {
                    visitMap.put("diagnosis", "Error decrypting diagnosis");
                }
                
                return visitMap;
            }).collect(Collectors.toList());
            
            // Audit log
            auditLogService.logDataAccess(authentication.getName(), "AllVisits", 
                null, "VIEW", true);
            
            return ResponseEntity.ok(visitList);
            
        } catch (Exception e) {
            logger.error("Error fetching all visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error fetching visits"));
        }
    }
    
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<?> getPatientVisits(@PathVariable Long patientId,
                                            Authentication authentication) {
        try {
            List<Patientvisit> visits = visitRepository.findByPatient_Id(patientId);
            
            // Decrypt diagnoses for authorized users
            List<Map<String, Object>> visitList = visits.stream().map(visit -> {
                Map<String, Object> visitMap = new HashMap<>();
                visitMap.put("id", visit.getVisitid());
                visitMap.put("visitDate", visit.getVisitdate());
                visitMap.put("doctorName", visit.getDoctor().getUsername());
                
                try {
                    if (visit.getEncryptedDiagnosis() != null) {
                        String decryptedDiagnosis = encryptionService.decrypt(visit.getEncryptedDiagnosis());
                        visitMap.put("diagnosis", decryptedDiagnosis);
                    } else {
                        visitMap.put("diagnosis", "No diagnosis recorded");
                    }
                } catch (Exception e) {
                    visitMap.put("diagnosis", "Error decrypting diagnosis");
                }
                
                return visitMap;
            }).collect(Collectors.toList());
            
            // Audit log
            auditLogService.logDataAccess(authentication.getName(), "PatientVisits", 
                patientId, "VIEW", true);
            
            return ResponseEntity.ok(visitList);
            
        } catch (Exception e) {
            logger.error("Error fetching patient visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error fetching visits"));
        }
    }
    
    @GetMapping("/my-visits")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getMyVisits(Authentication authentication) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            List<Patientvisit> visits = visitRepository.findByPatient_Id(principal.getId());
            
            // Patients can see their diagnosis but not edit
            List<Map<String, Object>> visitList = visits.stream().map(visit -> {
                Map<String, Object> visitMap = new HashMap<>();
                visitMap.put("id", visit.getVisitid());
                visitMap.put("visitDate", visit.getVisitdate());
                visitMap.put("doctorName", visit.getDoctor().getUsername());
                
                try {
                    if (visit.getEncryptedDiagnosis() != null) {
                        String decryptedDiagnosis = encryptionService.decrypt(visit.getEncryptedDiagnosis());
                        visitMap.put("diagnosis", decryptedDiagnosis);
                    } else {
                        visitMap.put("diagnosis", "No diagnosis recorded");
                    }
                } catch (Exception e) {
                    visitMap.put("diagnosis", "Error decrypting diagnosis");
                }
                
                return visitMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(visitList);
            
        } catch (Exception e) {
            logger.error("Error fetching patient's own visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error fetching visits"));
        }
    }
}
