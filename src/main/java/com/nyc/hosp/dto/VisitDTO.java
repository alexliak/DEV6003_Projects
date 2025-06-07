package com.nyc.hosp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VisitDTO {
    
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotBlank(message = "Diagnosis is required")
    @Size(min = 10, max = 5000, message = "Diagnosis must be between 10 and 5000 characters")
    private String diagnosis;
    
    public VisitDTO() {
    }
    
    public VisitDTO(Long patientId, String diagnosis) {
        this.patientId = patientId;
        this.diagnosis = diagnosis;
    }
    
    public Long getPatientId() {
        return patientId;
    }
    
    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
    
    public String getDiagnosis() {
        return diagnosis;
    }
    
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
