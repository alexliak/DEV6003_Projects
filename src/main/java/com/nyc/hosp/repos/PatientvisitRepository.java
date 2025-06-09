package com.nyc.hosp.repos;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Patientvisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PatientvisitRepository extends JpaRepository<Patientvisit, Integer> {

    Patientvisit findFirstByPatient(Hospuser hospuser);

    Patientvisit findFirstByDoctor(Hospuser hospuser);
    
    List<Patientvisit> findByPatient_Id(Long patientId);
    
    List<Patientvisit> findByDoctor_Id(Long doctorId);
    
    List<Patientvisit> findByPatientIdOrderByVisitdateDesc(Long patientId);
    
    @Query("SELECT pv FROM Patientvisit pv JOIN FETCH pv.patient JOIN FETCH pv.doctor ORDER BY pv.visitid")
    List<Patientvisit> findAllWithPatientAndDoctor();
    
    @Query("SELECT pv FROM Patientvisit pv LEFT JOIN FETCH pv.patient LEFT JOIN FETCH pv.doctor WHERE pv.visitid = :id")
    Optional<Patientvisit> findByIdWithPatientAndDoctor(@Param("id") Integer id);

}
