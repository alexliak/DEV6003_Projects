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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class HospuserService {

    private final HospuserRepository hospuserRepository;
    private final RoleRepository roleRepository;
    private final PatientvisitRepository patientvisitRepository;

    public HospuserService(final HospuserRepository hospuserRepository,
            final RoleRepository roleRepository,
            final PatientvisitRepository patientvisitRepository) {
        this.hospuserRepository = hospuserRepository;
        this.roleRepository = roleRepository;
        this.patientvisitRepository = patientvisitRepository;
    }

    public List<HospuserDTO> findAll() {
        final List<Hospuser> hospusers = hospuserRepository.findAll(Sort.by("id"));
        return hospusers.stream()
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
        mapToEntity(hospuserDTO, hospuser);
        hospuserRepository.save(hospuser);
    }

    public void delete(final Long userId) {
        hospuserRepository.deleteById(userId);
    }

    private HospuserDTO mapToDTO(final Hospuser hospuser, final HospuserDTO hospuserDTO) {
        hospuserDTO.setUserId(hospuser.getId());
        hospuserDTO.setUsername(hospuser.getUsername());
        hospuserDTO.setUserpassword(hospuser.getPassword());
        hospuserDTO.setLastlogondatetime(hospuser.getLastlogondatetime());
        hospuserDTO.setLastchangepassword(hospuser.getLastPasswordChange() != null ? 
            hospuser.getLastPasswordChange().toInstant().atOffset(java.time.ZoneOffset.UTC) : null);
        hospuserDTO.setEmail(hospuser.getEmail());
        hospuserDTO.setLocked(hospuser.isAccountLocked());
        
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
        hospuser.setPassword(hospuserDTO.getUserpassword());
        hospuser.setLastlogondatetime(hospuserDTO.getLastlogondatetime());
        hospuser.setEmail(hospuserDTO.getEmail());
        hospuser.setAccountLocked(hospuserDTO.isLocked());
        
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
