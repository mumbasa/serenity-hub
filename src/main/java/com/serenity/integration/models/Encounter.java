package com.serenity.integration.models;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



@Setter
@Getter
@ToString
@Entity
@Table(name = "encounter")
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    @Column(name = "encounter_class")
    private String encounterClass;

    @Column(name = "status")
    private String status;

    @Column(name = "display")
    private String display;

    @Column(name = "priority")
    private String priority;

    @Column(name = "planned_start")
    private String plannedStart;

    @Column(name = "planned_end")
    private String plannedEnd;

    @Column(name = "started_at")
    private String startedAt;

    @Column(name = "ended_at")
    private String endedAt;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_system")
    private String externalSystem;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "service_type_id")
    private String serviceTypeId;

    @Column(name = "service_type_name")
    private String serviceTypeName;

    @Column(name = "service_provider_id")
    private String serviceProviderId;

    @Column(name = "patient_mr_number")
    private String patientMrNumber;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "patient_full_name")
    private String patientFullName;

    @Column(name = "patient_mobile")
    private String patientMobile;

    @Column(name = "patient_birth_date")
    private String patientBirthDate;

    @Column(name = "patient_gender")
    private String patientGender;

    @Column(name = "patient_status")
    private String patientStatus;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "created_by_name")
    private String createdByName;

    @Column(name = "user_friendly_id")
    private String userFriendlyId;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "assigned_to_id")
    private String assignedToId;

    @Column(name = "service_provider_name")
    private String serviceProviderName;

    @Column(name = "visit_id")
    private String visitId;

    private boolean serviceRequest;
    private boolean prescription;

/* 
    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "encounter_d")
    private EncounterNote note; */

public Encounter (){}
    public Encounter(EncounterNote notes,Visits visit,PatientData p){
        this.prescription =false;
        this.serviceRequest=false;
        this.createdAt=notes.getEncounterDate();
        this.setEncounterClass(notes.getEncounterType());
        this.setAssignedToId(notes.getPractitionerId());
        this.setAssignedToName(notes.getPractitionerName());
        this.uuid=notes.getEncounterId();
        this.status="finished";
        this.priority="routine";
        this.setExternalId(notes.getExternalId());
        this.setExternalSystem(notes.getDataSource());
        this.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
        this.setServiceProviderName("Nyaho Medical Center");
        this.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
        this.setLocationName("Airport Primary Care");
        this.setVisitId(visit.getUuid().toString());
        this.setPatientBirthDate(p.getBirthDate());
        this.setPatientMrNumber(p.getMrNumber());
        this.setPatientGender(p.getGender());
        this.setPatientId(p.getUuid());
        this.setPatientFullName(p.getFullName());
        this.setDisplay(notes.getEncounterDate()+"-"+p.getMrNumber()+"-"+notes.getEncounterType());
        
    }
    public Encounter(EncounterNote notes,PatientData p){
        this.prescription =false;
        this.serviceRequest=false;
        this.createdAt=notes.getEncounterDate();
        this.setEncounterClass(notes.getEncounterType());
        this.setAssignedToId(notes.getPractitionerId());
        this.setAssignedToName(notes.getPractitionerName());
        this.uuid=notes.getEncounterId();
        this.status="finished";
        this.priority="routine";
        this.setExternalId(notes.getExternalId());
        this.setExternalSystem(notes.getDataSource());
        this.setServiceProviderId("161380e9-22d3-4627-a97f-0f918ce3e4a9");
        this.setServiceProviderName("Nyaho Medical Center");
        this.setLocationId("23f59485-8518-4f4e-9146-d061dfe58175");
        this.setLocationName("Airport Primary Care");
        this.setPatientBirthDate(p.getBirthDate());
        this.setPatientMrNumber(p.getMrNumber());
        this.setPatientGender(p.getGender());
        this.setPatientId(p.getUuid());
        this.setPatientFullName(p.getFullName());
        this.setDisplay(notes.getEncounterDate()+"-"+p.getMrNumber()+"-"+notes.getEncounterType());
        
        
    }



}
