package com.nyc.hosp.service;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import com.nyc.hosp.encryption.DiagnosisEncryptionService;
import com.nyc.hosp.model.PatientvisitDTO;
import com.nyc.hosp.repos.HospuserRepository;
import com.nyc.hosp.repos.PatientvisitRepository;
import com.nyc.hosp.util.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class PatientvisitService {

    private static final Logger logger = LoggerFactory.getLogger(PatientvisitService.class);
    
    private final PatientvisitRepository patientvisitRepository;
    private final HospuserRepository hospuserRepository;
    
    @Autowired
    private DiagnosisEncryptionService encryptionService;

    public PatientvisitService(final PatientvisitRepository patientvisitRepository,
            final HospuserRepository hospuserRepository) {
        this.patientvisitRepository = patientvisitRepository;
        this.hospuserRepository = hospuserRepository;
    }

    public List<PatientvisitDTO> findAll() {
        final List<Patientvisit> patientvisits = patientvisitRepository.findAll(Sort.by("visitid"));
        return patientvisits.stream()
                .map(patientvisit -> mapToDTO(patientvisit, new PatientvisitDTO()))
                .toList();
    }

    public PatientvisitDTO get(final Integer visitid) {
        return patientvisitRepository.findById(visitid)
                .map(patientvisit -> mapToDTO(patientvisit, new PatientvisitDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Integer create(final PatientvisitDTO patientvisitDTO) {
        final Patientvisit patientvisit = new Patientvisit();
        mapToEntity(patientvisitDTO, patientvisit);
        
        // Encrypt diagnosis before saving
        if (patientvisit.getDiagnosis() != null && !patientvisit.getDiagnosis().isEmpty()) {
            try {
                String encrypted = encryptionService.encrypt(patientvisit.getDiagnosis());
                patientvisit.setEncryptedDiagnosis(encrypted);
                patientvisit.setDiagnosis(null); // Clear plain text
            } catch (Exception e) {
                logger.error("Failed to encrypt diagnosis", e);
                throw new RuntimeException("Failed to process diagnosis");
            }
        }
        
        return patientvisitRepository.save(patientvisit).getVisitid();
    }

    public void update(final Integer visitid, final PatientvisitDTO patientvisitDTO) {
        final Patientvisit patientvisit = patientvisitRepository.findById(visitid)
                .orElseThrow(NotFoundException::new);
        mapToEntity(patientvisitDTO, patientvisit);
        
        // Encrypt diagnosis before saving
        if (patientvisit.getDiagnosis() != null && !patientvisit.getDiagnosis().isEmpty()) {
            try {
                String encrypted = encryptionService.encrypt(patientvisit.getDiagnosis());
                patientvisit.setEncryptedDiagnosis(encrypted);
                patientvisit.setDiagnosis(null); // Clear plain text
            } catch (Exception e) {
                logger.error("Failed to encrypt diagnosis", e);
                throw new RuntimeException("Failed to process diagnosis");
            }
        }
        
        patientvisitRepository.save(patientvisit);
    }

    public void delete(final Integer visitid) {
        patientvisitRepository.deleteById(visitid);
    }

    private PatientvisitDTO mapToDTO(final Patientvisit patientvisit,
            final PatientvisitDTO patientvisitDTO) {
        patientvisitDTO.setVisitid(patientvisit.getVisitid());
        patientvisitDTO.setVistidate(patientvisit.getVisitdate());
        
        // Decrypt diagnosis for display
        if (patientvisit.getEncryptedDiagnosis() != null && !patientvisit.getEncryptedDiagnosis().isEmpty()) {
            try {
                String decrypted = encryptionService.decrypt(patientvisit.getEncryptedDiagnosis());
                patientvisitDTO.setDiagnosis(decrypted);
            } catch (Exception e) {
                logger.error("Failed to decrypt diagnosis for visit {}", patientvisit.getVisitid(), e);
                patientvisitDTO.setDiagnosis("[Decryption Error]");
            }
        } else {
            patientvisitDTO.setDiagnosis(patientvisit.getDiagnosis());
        }
        
        patientvisitDTO.setPatient(patientvisit.getPatient() == null ? null : patientvisit.getPatient().getId());
        patientvisitDTO.setDoctor(patientvisit.getDoctor() == null ? null : patientvisit.getDoctor().getId());
        return patientvisitDTO;
    }

    private Patientvisit mapToEntity(final PatientvisitDTO patientvisitDTO,
            final Patientvisit patientvisit) {
        patientvisit.setVisitdate(patientvisitDTO.getVistidate());
        patientvisit.setDiagnosis(patientvisitDTO.getDiagnosis());
        final Hospuser patient = patientvisitDTO.getPatient() == null ? null : hospuserRepository.findById(patientvisitDTO.getPatient())
                .orElseThrow(() -> new NotFoundException("patient not found"));
        patientvisit.setPatient(patient);
        final Hospuser doctor = patientvisitDTO.getDoctor() == null ? null : hospuserRepository.findById(patientvisitDTO.getDoctor())
                .orElseThrow(() -> new NotFoundException("doctor not found"));
        patientvisit.setDoctor(doctor);
        return patientvisit;
    }

}
