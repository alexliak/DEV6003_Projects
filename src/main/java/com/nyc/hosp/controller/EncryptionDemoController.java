package com.nyc.hosp.controller;

import com.nyc.hosp.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/encryption-demo")
@PreAuthorize("hasRole('ADMIN')")
public class EncryptionDemoController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @GetMapping
    public String encryptionDemo(Model model) {
        // Show what's ACTUALLY in the database
        String query = """
            SELECT 
                visitid,
                diagnosis as plain_diagnosis,
                encrypted_diagnosis,
                LENGTH(encrypted_diagnosis) as encrypted_length,
                patient_id,
                doctor_id
            FROM patientvisit
            ORDER BY visitid DESC
            LIMIT 10
            """;
            
        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(query);
        
        // Show decryption process
        rawData.forEach(row -> {
            String encrypted = (String) row.get("encrypted_diagnosis");
            if (encrypted != null) {
                try {
                    // Try to decrypt
                    String decrypted = encryptionService.decrypt(encrypted);
                    row.put("decrypted_diagnosis", decrypted);
                    row.put("decryption_status", "SUCCESS");
                } catch (Exception e) {
                    row.put("decrypted_diagnosis", "[Cannot Decrypt]");
                    row.put("decryption_status", "FAILED: " + e.getMessage());
                }
            }
        });
        
        model.addAttribute("databaseContent", rawData);
        
        // Show encryption key info (but NOT the actual key!)
        model.addAttribute("encryptionAlgorithm", "AES-256-GCM");
        model.addAttribute("keyLocation", "application.properties (NOT in database)");
        
        return "admin/encryption-demo";
    }
}
