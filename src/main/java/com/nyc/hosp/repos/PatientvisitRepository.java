package com.nyc.hosp.repos;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientvisitRepository extends JpaRepository<Patientvisit, Integer> {

    Patientvisit findFirstByPatient(Hospuser hospuser);

    Patientvisit findFirstByDoctor(Hospuser hospuser);
    
    List<Patientvisit> findByPatient_Id(Long patientId);
    
    List<Patientvisit> findByDoctor_Id(Long doctorId);

}
