package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.security.CustomUserPrincipal;
import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/patient")
public class PatientDashboardController {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Autowired
    private PatientvisitRepository patientvisitRepository;
    
    @Autowired
    private DiagnosisEncryptionService encryptionService;
    
    @GetMapping("/my-records")
    public String myRecords(Authentication authentication, Model model) {
        try {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            Hospuser patient = hospuserRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get all visits for this patient
            List<Patientvisit> myVisits = patientvisitRepository.findByPatientIdOrderByVisitdateDesc(patient.getId());
            
            // Decrypt diagnoses for display
            myVisits.forEach(visit -> {
                if (visit.getEncryptedDiagnosis() != null) {
                    try {
                        visit.setDiagnosis(encryptionService.decrypt(visit.getEncryptedDiagnosis()));
                    } catch (Exception e) {
                        visit.setDiagnosis("[Decryption Error]");
                    }
                }
            });
            
            // Calculate age if date of birth exists
            if (patient.getDateOfBirth() != null) {
                long age = java.time.temporal.ChronoUnit.YEARS.between(
                    new java.sql.Date(patient.getDateOfBirth().getTime()).toLocalDate(),
                    java.time.LocalDate.now()
                );
                model.addAttribute("patientAge", age);
            }
            
            model.addAttribute("patient", patient);
            model.addAttribute("visits", myVisits);
            model.addAttribute("visitCount", myVisits.size());
            
            return "patient/my-records";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }
}
