package com.serenity.integration.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Entity
public class EncounterNote {
    @Id
   @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private long id;

    @Column(columnDefinition="TEXT")
    private String uuid;
    @Column(columnDefinition="TEXT")

    private String createdAt;
    @Column(columnDefinition="TEXT")

    private String updatedAt;
    @Column(columnDefinition="TEXT")
    private String note;
 
    @Column(columnDefinition="TEXT")
    private String encounterId;

    private String patientId;

    private String patientMobile;
    private String patientBirthDate;
    private String patientGender;
    private String patientStatus;
    private String patientFullName;
    @Column(columnDefinition="TEXT")
    private String encounterDate;
    @Column(columnDefinition="TEXT")

    private String patientMrNumber;

    @Column(columnDefinition="TEXT")
    private String encounterType;
    @Column(columnDefinition="TEXT")

    private String noteType;
    private boolean isEdited;
    private boolean isRecalled;
    private boolean isFormatted;
    @Column(columnDefinition="TEXT")

    private String practitionerName;
    @Column(columnDefinition="TEXT")

    private String practitionerRoleType;
    @Column(columnDefinition="TEXT")

    private String practitionerId;
    private String serviceProviderId;
    private String serviceProviderName;
    private String visitId;
    private String hisVisitId;
    private String locationId;
    private String locationName;
    private String externalId;
    private String externalSystem;


}
