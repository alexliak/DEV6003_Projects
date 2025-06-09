package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.model.PatientvisitDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.service.PatientvisitService;
import com.nyc.hosp.util.CustomCollectors;
import com.nyc.hosp.util.WebUtils;
import com.nyc.hosp.validation.InputSanitizer;
import com.nyc.hosp.audit.AuditLogService;
import com.nyc.hosp.security.SecurityEventLogger;
import jakarta.servlet.http.HttpServletRequest;
//import com.nyc.hosp.audit.EnhancedAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;


@Controller
@RequestMapping("/patientvisits")
public class PatientvisitController {

    private final PatientvisitService patientvisitService;
    private final HospuserRepository hospuserRepository;
    
    @Autowired
    private InputSanitizer inputSanitizer;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private SecurityEventLogger securityEventLogger;
    
    @Autowired
    private HttpServletRequest request;
    
    //@Autowired
    //private EnhancedAuditService enhancedAuditService;

    public PatientvisitController(final PatientvisitService patientvisitService,
            final HospuserRepository hospuserRepository) {
        this.patientvisitService = patientvisitService;
        this.hospuserRepository = hospuserRepository;
    }

    @ModelAttribute
    public void prepareContext(final Model model) {
        model.addAttribute("patientValues", hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_PATIENT)
                .stream()
                .collect(CustomCollectors.toSortedMap(Hospuser::getId, Hospuser::getUsername)));
        model.addAttribute("doctorValues", hospuserRepository.findByRoleName(com.nyc.hosp.domain.Role.RoleName.ROLE_DOCTOR)
                .stream()
                .collect(CustomCollectors.toSortedMap(Hospuser::getId, Hospuser::getUsername)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public String list(final Model model, Authentication authentication) {
        try {
            // ALL users (including doctors) see ALL visits
            model.addAttribute("patientvisits", patientvisitService.findAll());
            
            // Add current user info for view logic
            if (authentication != null) {
                model.addAttribute("currentUsername", authentication.getName());
                model.addAttribute("isAdmin", authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
                model.addAttribute("isDoctor", authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR")));
                model.addAttribute("isSecretary", authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARIAT")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("patientvisits", new ArrayList<>());
            model.addAttribute("MSG_ERROR", "Error loading visits: " + e.getMessage());
        }
        return "patientvisit/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("patientvisit") final PatientvisitDTO patientvisitDTO) {
        return "patientvisit/add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('DOCTOR')")
    public String add(@ModelAttribute("patientvisit") @Valid final PatientvisitDTO patientvisitDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "patientvisit/add";
        }
        
        // VALIDATE INPUT FOR SQL INJECTION AND XSS
        if (!inputSanitizer.isValid(patientvisitDTO.getDiagnosis())) {
            // Log security violation
            auditLogService.logSecurityViolation(
                authentication.getName(),
                "MALICIOUS_INPUT",
                "Attempted SQL injection or XSS in diagnosis",
                request.getRemoteAddr()
            );
            securityEventLogger.logEvent(
                "SECURITY_VIOLATION",
                authentication.getName(),
                "MALICIOUS_INPUT",
                "BLOCKED",
                "SQL injection or XSS detected in diagnosis field"
            );
            
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR, "Invalid input detected. This incident has been logged.");
            return "redirect:/patientvisits/add";
        }
        
        // Automatically set the doctor from the logged-in user
        String username = authentication.getName();
        Hospuser doctor = hospuserRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Doctor not found"));
        patientvisitDTO.setDoctor(doctor.getId().intValue());
        
        try {
            patientvisitService.create(patientvisitDTO);
            redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patientvisit.create.success"));
            return "redirect:/patientvisits";
        } catch (IllegalArgumentException e) {
            // This is thrown when malicious input is detected
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR, e.getMessage());
            return "redirect:/patientvisits/add";
        }
    }

    @GetMapping("/edit/{visitid}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public String edit(@PathVariable(name = "visitid") final Integer visitid, final Model model,
            Authentication authentication) {
        try {
            PatientvisitDTO visit = patientvisitService.get(visitid);
            
            // Check if doctor can edit this visit
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
                String username = authentication.getName();
                Hospuser doctor = hospuserRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
                
                if (!visit.getDoctor().equals(doctor.getId().intValue())) {
                    model.addAttribute("MSG_ERROR", "You can only edit your own visits");
                    return "redirect:/patientvisits";
                }
            }
            
            model.addAttribute("patientvisit", visit);
            
            // Debug info
            System.out.println("Edit visit " + visitid + " for user " + authentication.getName());
            System.out.println("Visit data: " + visit.getVisitid() + ", " + visit.getDiagnosis());
            
            return "patientvisit/edit";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("MSG_ERROR", "Error loading visit: " + e.getMessage());
            return "redirect:/patientvisits";
        }
    }

    @PostMapping("/edit/{visitid}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public String edit(@PathVariable(name = "visitid") final Integer visitid,
            @ModelAttribute("patientvisit") @Valid final PatientvisitDTO patientvisitDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "patientvisit/edit";
        }
        
        // VALIDATE INPUT FOR SQL INJECTION AND XSS
        if (!inputSanitizer.isValid(patientvisitDTO.getDiagnosis())) {
            // Log security violation
            auditLogService.logSecurityViolation(
                authentication.getName(),
                "MALICIOUS_INPUT",
                "Attempted SQL injection or XSS in diagnosis during edit",
                request.getRemoteAddr()
            );
            securityEventLogger.logEvent(
                "SECURITY_VIOLATION",
                authentication.getName(),
                "MALICIOUS_INPUT",
                "BLOCKED",
                "SQL injection or XSS detected in diagnosis field during edit"
            );
            
            redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR, "Invalid input detected. This incident has been logged.");
            return "redirect:/patientvisits/edit/" + visitid;
        }
        
        // Sanitize the diagnosis
        patientvisitDTO.setDiagnosis(inputSanitizer.sanitize(patientvisitDTO.getDiagnosis()));
        
        // Check if doctor can edit this visit
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"))) {
            PatientvisitDTO existingVisit = patientvisitService.get(visitid);
            String username = authentication.getName();
            Hospuser doctor = hospuserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
            
            if (!existingVisit.getDoctor().equals(doctor.getId().intValue())) {
                redirectAttributes.addFlashAttribute(WebUtils.MSG_ERROR, "You can only edit your own visits");
                return "redirect:/patientvisits";
            }
            
            // Ensure doctor ID doesn't change
            patientvisitDTO.setDoctor(doctor.getId().intValue());
        }
        
        patientvisitService.update(visitid, patientvisitDTO);
        
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("patientvisit.update.success"));
        return "redirect:/patientvisits";
    }

    @PostMapping("/delete/{visitid}")
    public String delete(@PathVariable(name = "visitid") final Integer visitid,
            final RedirectAttributes redirectAttributes) {
        patientvisitService.delete(visitid);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("patientvisit.delete.success"));
        return "redirect:/patientvisits";
    }

}
