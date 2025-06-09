package com.nyc.hosp.controller;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/patients")
@PreAuthorize("hasAnyRole('SECRETARIAT', 'ADMIN')")
public class PatientManagementController {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @GetMapping
    public String listPatients(Model model) {
        // Get only patients
        List<Hospuser> patients = hospuserRepository.findByRoleName(
            com.nyc.hosp.domain.Role.RoleName.ROLE_PATIENT
        );
        
        model.addAttribute("patients", patients);
        return "patient/list";
    }
    
    @GetMapping("/appointments")
    public String manageAppointments(Model model) {
        // Secretary can manage appointments but not see medical details
        List<Hospuser> patients = hospuserRepository.findByRoleName(
            com.nyc.hosp.domain.Role.RoleName.ROLE_PATIENT
        );
        List<Hospuser> doctors = hospuserRepository.findByRoleName(
            com.nyc.hosp.domain.Role.RoleName.ROLE_DOCTOR
        );
        
        model.addAttribute("patients", patients);
        model.addAttribute("doctors", doctors);
        return "patient/appointments";
    }
}
