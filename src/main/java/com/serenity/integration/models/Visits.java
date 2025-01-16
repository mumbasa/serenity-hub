package com.serenity.integration.models;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Entity
@Table
@ToString
public class Visits {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private UUID uuid;
    private String externalId;
    private String externalSystem;
    private String createdAt;
    private String status;
    private String userFriendlyId;
    private String encounterClass;
    private String display;
    private String priority;
    private String startedAt;
    private String endedAt;
    private String locationId;
    private String locationName;
    private String assignedToName;
    private String assignedToId;
    private String serviceProviderId;
    private String patientId;
    private String serviceProviderName;
    private String patientName;
    private String patientMobile;
    private String patientMrNumber;
    private String patientDob;
    private String gender;
    private String hisNumber;
    private String patientStatus;
    private String practitionerId;

    public Visits(EncounterNote note, UUID uuid, PatientData data) {
        this.externalSystem = note.getExternalSystem();
        this.uuid = UUID.fromString(note.getVisitId());
        this.createdAt = note.getEncounterDate();
        this.serviceProviderId = "161380e9-22d3-4627-a97f-0f918ce3e4a9";
        this.serviceProviderName = "Nyaho Medical Center";
        this.encounterClass = note.getEncounterType();
        this.patientDob = data.getBirthDate();
        this.patientId = data.getUuid();
        this.patientMrNumber = data.getMrNumber(); // Fixed method name
        this.gender = data.getGender();
        this.assignedToName = note.getPractitionerName(); // Fixed get/set
        this.assignedToId = note.getPractitionerId();
        this.setPatientName(data.getFullName());
        this.setDisplay("HIS-Visit-"+data.getMrNumber());
    }
    public Visits(EncounterNote note) {
        this.externalSystem = note.getExternalSystem();
        this.uuid = UUID.fromString(note.getVisitId());
        this.createdAt = note.getEncounterDate();
        this.serviceProviderId = "161380e9-22d3-4627-a97f-0f918ce3e4a9";
        this.serviceProviderName = "Nyaho Medical Center";
        this.encounterClass = note.getEncounterType();
        this.patientDob = note.getPatientBirthDate();
        this.patientId = note.getPatientId();
        this.patientMrNumber = note.getPatientMrNumber(); // Fixed method name
        this.gender = note.getPatientGender();
        this.assignedToName = note.getPractitionerName(); // Fixed get/set
        this.assignedToId = note.getPractitionerId();
        this.setPatientName(note.getPatientFullName());
        this.setDisplay("HIS-Visit-"+note.getPatientMrNumber());
    }

    public Visits() {

    }

    public Visits(Encounter encounter) {
        this.externalSystem = encounter.getExternalSystem();
        this.uuid = UUID.fromString(encounter.getVisitId());
        this.createdAt = encounter.getCreatedAt();
        this.serviceProviderId = "161380e9-22d3-4627-a97f-0f918ce3e4a9";
        this.serviceProviderName = "Nyaho Medical Center";
        this.encounterClass = encounter.getEncounterClass();
        this.patientDob = encounter.getPatientBirthDate();
        this.patientId = encounter.getPatientId();
        this.patientMrNumber = encounter.getPatientMrNumber(); // Fixed method name
        this.gender = encounter.getPatientGender();
        this.assignedToName = encounter.getAssignedToName(); // Fixed get/set
        this.assignedToId = encounter.getAssignedToId();
        this.setPatientName(encounter.getPatientFullName());
        this.setDisplay("HIS-Visit-"+encounter.getPatientMrNumber());
    }
}
