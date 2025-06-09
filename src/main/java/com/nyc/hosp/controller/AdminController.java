package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.util.LoggingUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private HospuserRepository hospuserRepository;

    @Autowired
    private LoggingUtil loggingUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/users/password-history")
    public String viewPasswordHistory(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Authentication authentication,
                                    Model model) {
        try {
            // Log admin access
            loggingUtil.logSecurityEvent("ADMIN_VIEW_PASSWORD_HISTORY", authentication.getName(), true);

            // Get paginated users
            Page<Hospuser> usersPage = hospuserRepository.findAll(
                PageRequest.of(page, size, Sort.by("username").ascending())
            );

            // Process password history for display
            List<Map<String, Object>> userPasswordData = new ArrayList<>();

            for (Hospuser user : usersPage.getContent()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("lastPasswordChange", user.getLastPasswordChange());
                userData.put("forcePasswordChange", user.isForcePasswordChange());

                // Calculate days since last change
                if (user.getLastPasswordChange() != null) {
                    long daysSinceChange = java.time.temporal.ChronoUnit.DAYS.between(
                        user.getLastPasswordChange().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate(),
                        java.time.LocalDate.now()
                    );
                    userData.put("daysSinceChange", daysSinceChange);
                    userData.put("passwordExpired", daysSinceChange > 90);
                }

                // Parse password history
                List<String> passwordHashes = new ArrayList<>();
                if (user.getPasswordHistory() != null && !user.getPasswordHistory().isEmpty()) {
                    try {
                        passwordHashes = objectMapper.readValue(
                            user.getPasswordHistory(),
                            new TypeReference<List<String>>() {}
                        );
                    } catch (Exception e) {
                        // If parsing fails, show empty history
                    }
                }
                userData.put("passwordHistoryCount", passwordHashes.size());
                userData.put("passwordHashes", passwordHashes);

                userPasswordData.add(userData);
            }

            model.addAttribute("users", userPasswordData);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalUsers", usersPage.getTotalElements());

            return "admin/password-history";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error loading password history data");
            return "admin/password-history";
        }
    }

    @PostMapping("/users/{userId}/force-password-change")
    public String forcePasswordChange(@PathVariable Long userId,
                                    Authentication authentication,
                                    Model model) {
        try {
            Hospuser user = hospuserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            user.setForcePasswordChange(true);
            hospuserRepository.save(user);

            loggingUtil.logSecurityEvent("ADMIN_FORCE_PASSWORD_CHANGE",
                authentication.getName() + " -> " + user.getUsername(), true);

            model.addAttribute("MSG_SUCCESS",
                "User " + user.getUsername() + " will be required to change password on next login");

        } catch (Exception e) {
            model.addAttribute("MSG_ERROR", "Error: " + e.getMessage());
        }

        return "redirect:/admin/users/password-history";
    }

    @GetMapping("")
    public String adminHome(Model model) {
        return "redirect:/admin/audit-logs";
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Admin controller is working!";
    }

    @GetMapping("/audit-logs")
    public String viewAuditLogs(Model model, Authentication authentication) {
        try {
            // Get real-time logs from database
            String query = """
                SELECT 
                    id,
                    timestamp,
                    username,
                    event_type,
                    event_description,
                    entity_type,
                    entity_id,
                    success,
                    ip_address,
                    target_user,
                    DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as formattedTimestamp
                FROM audit_log
                ORDER BY timestamp DESC
                LIMIT 100
                """;
                
            List<Map<String, Object>> auditLogs = jdbcTemplate.queryForList(query);

            List<String> eventTypes = jdbcTemplate.queryForList(
                "SELECT DISTINCT event_type FROM audit_log",
                String.class
            );

            model.addAttribute("auditLogs", auditLogs);
            model.addAttribute("eventTypes", eventTypes);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalLogs", auditLogs.size());

            return "admin/audit-logs";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }
    
    @GetMapping("/audit-logs/refresh")
    @ResponseBody
    public List<Map<String, Object>> refreshLogs(@RequestParam(defaultValue = "50") int limit) {
        String query = """
            SELECT 
                id,
                timestamp,
                username,
                event_type,
                event_description,
                entity_type,
                entity_id,
                success,
                ip_address,
                target_user,
                DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as formattedTimestamp
            FROM audit_log
            ORDER BY timestamp DESC
            LIMIT ?
            """;
            
        return jdbcTemplate.queryForList(query, limit);
    }
    
    @GetMapping("/audit-logs/live")
    @ResponseBody
    public List<Map<String, Object>> getLiveLogs(@RequestParam(required = false) Long afterId) {
        String query;
        List<Map<String, Object>> logs;
        
        if (afterId != null) {
            // Get only new logs after the specified ID
            query = """
                SELECT 
                    id,
                    timestamp,
                    username,
                    event_type,
                    event_description,
                    entity_type,
                    entity_id,
                    success,
                    ip_address,
                    target_user,
                    DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as formattedTimestamp
                FROM audit_log
                WHERE id > ?
                ORDER BY id DESC
                LIMIT 20
                """;
            logs = jdbcTemplate.queryForList(query, afterId);
        } else {
            // Get latest 20 logs
            query = """
                SELECT 
                    id,
                    timestamp,
                    username,
                    event_type,
                    event_description,
                    entity_type,
                    entity_id,
                    success,
                    ip_address,
                    target_user,
                    DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i:%s') as formattedTimestamp
                FROM audit_log
                ORDER BY id DESC
                LIMIT 20
                """;
            logs = jdbcTemplate.queryForList(query);
        }
        
        return logs;
    }
}
