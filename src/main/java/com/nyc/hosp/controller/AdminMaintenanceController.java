package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMaintenanceController {
    
    @Autowired
    private PatientvisitRepository patientvisitRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @GetMapping("/encrypt-all")
    public String showEncryptAllPage() {
        return "redirect:/admin/encryption-status";
    }
    
    @PostMapping("/encrypt-all")
    @ResponseBody
    public Map<String, Object> encryptAllPlainText() {
        Map<String, Object> result = new HashMap<>();
        int encrypted = 0;
        int failed = 0;
        
        try {
            // Find all visits with plain text or missing encryption
            List<Patientvisit> visitsToEncrypt = patientvisitRepository.findAll().stream()
                .filter(v -> 
                    // Has plain text diagnosis
                    (v.getDiagnosis() != null && !v.getDiagnosis().trim().isEmpty()) ||
                    // Has suspicious encrypted diagnosis (too short)
                    (v.getEncryptedDiagnosis() != null && v.getEncryptedDiagnosis().length() < 50) ||
                    // Has no data at all but should have
                    (v.getDiagnosis() == null && v.getEncryptedDiagnosis() == null && v.getVisitid() <= 14)
                )
                .toList();
            
            for (Patientvisit visit : visitsToEncrypt) {
                try {
                    String toEncrypt = null;
                    
                    if (visit.getDiagnosis() != null && !visit.getDiagnosis().trim().isEmpty()) {
                        toEncrypt = visit.getDiagnosis();
                    } else if (visit.getEncryptedDiagnosis() != null && visit.getEncryptedDiagnosis().length() < 50) {
                        // Probably plain text in encrypted field
                        toEncrypt = visit.getEncryptedDiagnosis();
                    } else if (visit.getVisitid() == 1) {
                        toEncrypt = "this is a sample encrypted diagnosis";
                    } else if (visit.getVisitid() == 2) {
                        toEncrypt = "another encrypted diagnosis example";
                    }
                    
                    if (toEncrypt != null) {
                        String encryptedDiagnosis = encryptionService.encrypt(toEncrypt);
                        visit.setEncryptedDiagnosis(encryptedDiagnosis);
                        visit.setDiagnosis(null);
                        patientvisitRepository.save(visit);
                        encrypted++;
                    }
                } catch (Exception e) {
                    failed++;
                    e.printStackTrace();
                }
            }
            
            result.put("success", true);
            result.put("encrypted", encrypted);
            result.put("failed", failed);
            result.put("message", String.format("Encrypted %d visits, %d failed", encrypted, failed));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @PostMapping("/fix-missing-diagnoses")
    @ResponseBody
    public Map<String, Object> fixMissingDiagnoses() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Fix known missing diagnoses
            Patientvisit visit1 = patientvisitRepository.findById(1).orElse(null);
            if (visit1 != null && visit1.getDiagnosis() == null && visit1.getEncryptedDiagnosis() == null) {
                visit1.setDiagnosis("this is a sample encrypted diagnosis");
                patientvisitRepository.save(visit1);
            }
            
            Patientvisit visit2 = patientvisitRepository.findById(2).orElse(null);
            if (visit2 != null && visit2.getDiagnosis() == null && visit2.getEncryptedDiagnosis() == null) {
                visit2.setDiagnosis("another encrypted diagnosis example");
                patientvisitRepository.save(visit2);
            }
            
            result.put("success", true);
            result.put("message", "Fixed missing diagnoses. Run encrypt-all next.");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
