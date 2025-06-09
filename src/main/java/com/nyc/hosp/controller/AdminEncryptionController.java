package com.nyc.hosp.controller;

import com.nyc.hosp.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/encryption-status")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEncryptionController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @GetMapping
    public String encryptionStatus(Model model) {
        // Get all visits with raw data
        String query = """
            SELECT 
                visitid,
                diagnosis,
                encrypted_diagnosis,
                patient_id,
                doctor_id
            FROM patientvisit
            ORDER BY visitid DESC
            """;
            
        List<Map<String, Object>> visits = jdbcTemplate.queryForList(query);
        
        // Process each visit
        List<Map<String, Object>> processedVisits = new ArrayList<>();
        int encryptedCount = 0;
        int notEncryptedCount = 0;
        int suspiciousCount = 0;
        
        for (Map<String, Object> visit : visits) {
            Map<String, Object> processed = new HashMap<>();
            processed.put("visitId", visit.get("visitid"));
            processed.put("plainDiagnosis", visit.get("diagnosis"));
            processed.put("encryptedDiagnosis", visit.get("encrypted_diagnosis"));
            
            String encrypted = (String) visit.get("encrypted_diagnosis");
            String plain = (String) visit.get("diagnosis");
            
            if (plain != null) {
                // Plain text exists - BAD!
                processed.put("status", "NOT_ENCRYPTED");
                processed.put("decryptedValue", plain);
                notEncryptedCount++;
            } else if (encrypted != null) {
                // Check if it's properly encrypted
                if (encrypted.length() < 50 || (!encrypted.contains("/") && !encrypted.contains("+"))) {
                    // Suspicious - might be plain text in encrypted field
                    processed.put("status", "SUSPICIOUS");
                    processed.put("decryptedValue", encrypted);
                    suspiciousCount++;
                } else {
                    // Try to decrypt
                    try {
                        String decrypted = encryptionService.decrypt(encrypted);
                        processed.put("status", "ENCRYPTED");
                        processed.put("decryptedValue", decrypted);
                        encryptedCount++;
                    } catch (Exception e) {
                        processed.put("status", "DECRYPT_FAILED");
                        processed.put("decryptedValue", "[Decryption Failed]");
                        suspiciousCount++;
                    }
                }
            } else {
                processed.put("status", "NO_DATA");
                notEncryptedCount++;
            }
            
            processedVisits.add(processed);
        }
        
        model.addAttribute("visits", processedVisits);
        model.addAttribute("encryptedCount", encryptedCount);
        model.addAttribute("notEncryptedCount", notEncryptedCount);
        model.addAttribute("suspiciousCount", suspiciousCount);
        
        int total = visits.size();
        double percentage = total > 0 ? (encryptedCount * 100.0 / total) : 0;
        model.addAttribute("encryptionPercentage", String.format("%.1f", percentage));
        
        return "admin/encryption-status";
    }
    
    @PostMapping("/encrypt-all")
    @ResponseBody
    public Map<String, Object> encryptAll() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Find all plain text diagnoses
            String findQuery = "SELECT visitid, diagnosis FROM patientvisit WHERE diagnosis IS NOT NULL";
            List<Map<String, Object>> plainTextVisits = jdbcTemplate.queryForList(findQuery);
            
            int encrypted = 0;
            for (Map<String, Object> visit : plainTextVisits) {
                Integer visitId = (Integer) visit.get("visitid");
                String diagnosis = (String) visit.get("diagnosis");
                
                String encryptedDiagnosis = encryptionService.encrypt(diagnosis);
                
                String updateQuery = "UPDATE patientvisit SET encrypted_diagnosis = ?, diagnosis = NULL WHERE visitid = ?";
                jdbcTemplate.update(updateQuery, encryptedDiagnosis, visitId);
                encrypted++;
            }
            
            // Also fix suspicious entries (plain text in encrypted field)
            String fixQuery = """
                SELECT visitid, encrypted_diagnosis 
                FROM patientvisit 
                WHERE encrypted_diagnosis IS NOT NULL 
                AND LENGTH(encrypted_diagnosis) < 50
                """;
            List<Map<String, Object>> suspiciousVisits = jdbcTemplate.queryForList(fixQuery);
            
            for (Map<String, Object> visit : suspiciousVisits) {
                Integer visitId = (Integer) visit.get("visitid");
                String plainText = (String) visit.get("encrypted_diagnosis");
                
                String properlyEncrypted = encryptionService.encrypt(plainText);
                
                String updateQuery = "UPDATE patientvisit SET encrypted_diagnosis = ? WHERE visitid = ?";
                jdbcTemplate.update(updateQuery, properlyEncrypted, visitId);
                encrypted++;
            }
            
            result.put("success", true);
            result.put("encrypted", encrypted);
            result.put("message", "Successfully encrypted " + encrypted + " diagnoses");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
