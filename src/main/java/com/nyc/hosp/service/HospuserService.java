package com.nyc.hosp.service;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.domain.Role;
import com.nyc.hosp.model.HospuserDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.repos.RoleRepository;
import com.nyc.hosp.util.NotFoundException;
import com.nyc.hosp.util.ReferencedWarning;
import com.nyc.hosp.service.EmailService;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class HospuserService {
    
    private static final Logger logger = LoggerFactory.getLogger(HospuserService.class);

    private final HospuserRepository hospuserRepository;
    private final RoleRepository roleRepository;
    private final PatientvisitRepository patientvisitRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public HospuserService(final HospuserRepository hospuserRepository,
            final RoleRepository roleRepository,
            final PatientvisitRepository patientvisitRepository,
            final PasswordEncoder passwordEncoder,
            final EmailService emailService) {
        this.hospuserRepository = hospuserRepository;
        this.roleRepository = roleRepository;
        this.patientvisitRepository = patientvisitRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public List<HospuserDTO> findAll() {
        final List<Hospuser> hospusers = hospuserRepository.findAll(Sort.by("id"));
        return hospusers.stream()
                .map(hospuser -> mapToDTO(hospuser, new HospuserDTO()))
                .toList();
    }
    
    public List<HospuserDTO> findPatientsAndDoctors() {
        final List<Hospuser> allUsers = hospuserRepository.findAll(Sort.by("id"));
        return allUsers.stream()
                .filter(user -> {
                    // Filter only users with PATIENT or DOCTOR roles
                    return user.getRoles().stream().anyMatch(role -> 
                        role.getName() == Role.RoleName.ROLE_PATIENT || 
                        role.getName() == Role.RoleName.ROLE_DOCTOR
                    );
                })
                .map(hospuser -> mapToDTO(hospuser, new HospuserDTO()))
                .toList();
    }

    public HospuserDTO get(final Long userId) {
        return hospuserRepository.findById(userId)
                .map(hospuser -> mapToDTO(hospuser, new HospuserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final HospuserDTO hospuserDTO) {
        final Hospuser hospuser = new Hospuser();
        mapToEntity(hospuserDTO, hospuser);
        return hospuserRepository.save(hospuser).getId();
    }

    public void update(final Long userId, final HospuserDTO hospuserDTO) {
        final Hospuser hospuser = hospuserRepository.findById(userId)
                .orElseThrow(NotFoundException::new);
        
        // Store the current password before mapping
        String currentPassword = hospuser.getPassword();
        boolean wasPasswordChanged = false;
        String temporaryPassword = null;
        
        // Check if admin is providing a new password
        if (hospuserDTO.getUserpassword() != null && !hospuserDTO.getUserpassword().isEmpty() 
                && !hospuserDTO.getUserpassword().equals("********")) {
            // Generate a temporary password
            temporaryPassword = generateTemporaryPassword();
            hospuserDTO.setUserpassword(temporaryPassword);
            wasPasswordChanged = true;
        }
        
        mapToEntity(hospuserDTO, hospuser);
        
        // If no password was provided in the DTO, keep the existing password
        if (hospuserDTO.getUserpassword() == null || hospuserDTO.getUserpassword().isEmpty() 
                || hospuserDTO.getUserpassword().equals("********")) {
            hospuser.setPassword(currentPassword);
        }
        
        // Force password change if admin changed it
        if (wasPasswordChanged) {
            hospuser.setForcePasswordChange(true);
            logger.info("User {} marked for forced password change", hospuser.getUsername());
        }
        
        hospuserRepository.save(hospuser);
        
        // Send email if password was changed by admin
        if (wasPasswordChanged && temporaryPassword != null) {
            emailService.sendTemporaryPasswordEmail(
                hospuser.getEmail(), 
                hospuser.getUsername(), 
                temporaryPassword
            );
            logger.info("Temporary password sent to user: {}", hospuser.getUsername());
        }
    }
    
    private String generateTemporaryPassword() {
        // Generate a secure temporary password
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "Temp@" + uuid;
    }

    public void delete(final Long userId) {
        hospuserRepository.deleteById(userId);
    }

    private HospuserDTO mapToDTO(final Hospuser hospuser, final HospuserDTO hospuserDTO) {
        hospuserDTO.setUserId(hospuser.getId());
        hospuserDTO.setUsername(hospuser.getUsername());
        hospuserDTO.setUserpassword(hospuser.getPassword()); // Show the BCrypt hash
        hospuserDTO.setLastlogondatetime(hospuser.getLastlogondatetime());
        hospuserDTO.setLastchangepassword(hospuser.getLastPasswordChange() != null ? 
            hospuser.getLastPasswordChange().toInstant().atOffset(java.time.ZoneOffset.UTC) : null);
        hospuserDTO.setEmail(hospuser.getEmail());
        hospuserDTO.setLocked(hospuser.isAccountLocked());
        hospuserDTO.setFirstName(hospuser.getFirstName());
        hospuserDTO.setLastName(hospuser.getLastName());
        hospuserDTO.setDateOfBirth(hospuser.getDateOfBirth());
        
        // Handle multiple roles
        if (hospuser.getRoles() != null && !hospuser.getRoles().isEmpty()) {
            // For backward compatibility, get the first role
            Role firstRole = hospuser.getRoles().iterator().next();
            hospuserDTO.setRole(firstRole.getId().intValue());
        }
        
        return hospuserDTO;
    }

    private Hospuser mapToEntity(final HospuserDTO hospuserDTO, final Hospuser hospuser) {
        hospuser.setUsername(hospuserDTO.getUsername());
        
        // Hash the password only if it's being changed (not already hashed)
        String password = hospuserDTO.getUserpassword();
        if (password != null && !password.isEmpty()) {
            // Check if password is already hashed (BCrypt format starts with $2a$, $2b$, or $2y$)
            if (!password.startsWith("$2a$") && !password.startsWith("$2b$") && !password.startsWith("$2y$")) {
                // Password is plain text, hash it
                hospuser.setPassword(passwordEncoder.encode(password));
                // Set password change date when password is set
                hospuser.setLastPasswordChange(new Date());
            } else {
                // Password is already hashed, use as is
                hospuser.setPassword(password);
            }
        }
        
        hospuser.setLastlogondatetime(hospuserDTO.getLastlogondatetime());
        hospuser.setEmail(hospuserDTO.getEmail());
        hospuser.setAccountLocked(hospuserDTO.isLocked());
        hospuser.setFirstName(hospuserDTO.getFirstName());
        hospuser.setLastName(hospuserDTO.getLastName());
        hospuser.setDateOfBirth(hospuserDTO.getDateOfBirth());
        
        // Don't override lastPasswordChange if provided in DTO
        if (hospuserDTO.getLastchangepassword() != null) {
            hospuser.setLastPasswordChange(java.util.Date.from(
                hospuserDTO.getLastchangepassword().toInstant()));
        }
        
        // Handle single role from DTO to set of roles
        if (hospuserDTO.getRole() != null) {
            final Role role = roleRepository.findById(hospuserDTO.getRole().longValue())
                    .orElseThrow(() -> new NotFoundException("role not found"));
            hospuser.getRoles().clear();
            hospuser.getRoles().add(role);
        }
        
        return hospuser;
    }

    public ReferencedWarning getReferencedWarning(final Long userId) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Hospuser hospuser = hospuserRepository.findById(userId)
                .orElseThrow(NotFoundException::new);
        final Patientvisit patientPatientvisit = patientvisitRepository.findFirstByPatient(hospuser);
        if (patientPatientvisit != null) {
            referencedWarning.setKey("hospuser.patientvisit.patient.referenced");
            referencedWarning.addParam(patientPatientvisit.getVisitid());
            return referencedWarning;
        }
        final Patientvisit doctorPatientvisit = patientvisitRepository.findFirstByDoctor(hospuser);
        if (doctorPatientvisit != null) {
            referencedWarning.setKey("hospuser.patientvisit.doctor.referenced");
            referencedWarning.addParam(doctorPatientvisit.getVisitid());
            return referencedWarning;
        }
        return null;
    }
}
