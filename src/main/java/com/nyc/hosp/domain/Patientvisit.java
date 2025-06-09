package com.nyc.hosp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.LocalDate;


@Entity
public class Patientvisit {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer visitid;

    @Column
    private LocalDate visitdate;

    @Column(columnDefinition = "TEXT")
    private String diagnosis; // Store plain text for now

    @Column(columnDefinition = "TEXT")
    private String encryptedDiagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Hospuser patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Hospuser doctor;

    public Integer getVisitid() {
        return visitid;
    }

    public void setVisitid(final Integer visitid) {
        this.visitid = visitid;
    }

    public LocalDate getVisitdate() {
        return visitdate;
    }

    public void setVisitdate(final LocalDate vistidate) {
        this.visitdate = vistidate;
    }

    public String getEncryptedDiagnosis() {
        return encryptedDiagnosis;
    }

    public void setEncryptedDiagnosis(String encryptedDiagnosis) {
        this.encryptedDiagnosis = encryptedDiagnosis;
    }
    
    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(final String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Hospuser getPatient() {
        return patient;
    }

    public void setPatient(final Hospuser patient) {
        this.patient = patient;
    }

    public Hospuser getDoctor() {
        return doctor;
    }

    public void setDoctor(final Hospuser doctor) {
        this.doctor = doctor;
    }

}
