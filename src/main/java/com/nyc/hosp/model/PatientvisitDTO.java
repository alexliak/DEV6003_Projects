package com.nyc.hosp.model;

import java.time.LocalDate;


public class PatientvisitDTO {

    private Integer visitid;
    private LocalDate vistidate;
    private String diagnosis;
    private Long patient;
    private Long doctor;

    public Integer getVisitid() {
        return visitid;
    }

    public void setVisitid(final Integer visitid) {
        this.visitid = visitid;
    }

    public LocalDate getVistidate() {
        return vistidate;
    }

    public void setVistidate(final LocalDate vistidate) {
        this.vistidate = vistidate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(final String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Long getPatient() {
        return patient;
    }

    public void setPatient(final Long patient) {
        this.patient = patient;
    }

    public Long getDoctor() {
        return doctor;
    }

    public void setDoctor(final Long doctor) {
        this.doctor = doctor;
    }

}
