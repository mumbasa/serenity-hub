package com.serenity.integration.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private String uuid;
    private String createdAt;
    private String updatedAt;
        @Column(columnDefinition="TEXT")
    private String note;
    private String encounterId;
    private String encounterDate;
    private String patientMrNumber;
    private String encounterType;
    private String noteType;
    private boolean isEdited;
    private boolean isRecalled;
    private boolean isFormatted;
    private String practitionerName;
    private String practitionerRoleType;
    private String practitionerId;
    private String dataSource;
}
