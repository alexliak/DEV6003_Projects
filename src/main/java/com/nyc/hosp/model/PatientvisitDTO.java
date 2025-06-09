package com.nyc.hosp.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;


public class PatientvisitDTO {

    private Integer visitid;

    @NotNull
    private LocalDate visitdate;

    @Size(max = 1000)
    private String diagnosis;

    private Integer patient;

    private Integer doctor;
    
    // Additional display fields
    private String patientName;
    private String doctorName;
    private String doctorUsername;
    private Integer patientAge;
    private String formattedVisitDate;

    public Integer getVisitid() {
        return visitid;
    }

    public void setVisitid(final Integer visitid) {
        this.visitid = visitid;
    }

    public LocalDate getVisitdate() {
        return visitdate;
    }

    public void setVisitdate(final LocalDate visitdate) {
        this.visitdate = visitdate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(final String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Integer getPatient() {
        return patient;
    }

    public void setPatient(final Integer patient) {
        this.patient = patient;
    }

    public Integer getDoctor() {
        return doctor;
    }

    public void setDoctor(final Integer doctor) {
        this.doctor = doctor;
    }
    
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
    
    public Integer getPatientAge() {
        return patientAge;
    }
    
    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }
    
    public String getFormattedVisitDate() {
        return formattedVisitDate;
    }
    
    public void setFormattedVisitDate(String formattedVisitDate) {
        this.formattedVisitDate = formattedVisitDate;
    }
    
    public String getDoctorUsername() {
        return doctorUsername;
    }
    
    public void setDoctorUsername(String doctorUsername) {
        this.doctorUsername = doctorUsername;
    }

}
