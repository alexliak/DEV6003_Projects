package com.nyc.hosp.model;

import java.time.LocalDate;

public class PatientvisitDisplayDTO extends PatientvisitDTO {
    
    private String patientName;
    private String doctorName;
    private boolean canEdit;  // For checking if current doctor can edit
    
    public String getPatientName() {
        return patientName;
    }
    
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    
    public String getDoctorName() {
        return doctorName;
    }
    
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
    
    public boolean isCanEdit() {
        return canEdit;
    }
    
    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }
}
